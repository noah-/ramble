package ramble.messagesync;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.protobuf.ByteString;
import org.apache.commons.lang.mutable.MutableLong;
import org.apache.log4j.Logger;
import ramble.api.MembershipService;
import ramble.api.MessageSyncProtocol;
import ramble.api.RambleMember;
import ramble.api.RambleMessage;
import ramble.crypto.MessageSigner;
import ramble.db.api.DbStore;
import ramble.messagesync.api.MessageClientSyncHandler;
import ramble.messagesync.api.MessageSyncClient;
import ramble.messagesync.api.TargetSelector;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * An anti-entropy protocol iteratively syncs its messageQueue with a target random node. This is done by dividing all local
 * messageQueue into logical blocks that are bound by a time window. THe initiating starts at the most recent block, and
 * sends all messageQueue digests in the block to the target node. The target node responds with a list of messageQueue that the
 * source node is missing from the block in question. The source node then adds all these messageQueue to its local db, and
 * then moves to the next block.
 */
public class ComputeComplementService extends AbstractScheduledService implements Service {

  private static final Logger LOG = Logger.getLogger(ComputeComplementService.class);

  private static final int MIN_COUNT_FOR_CONFIRM = 3;
  private static final long BLOCK_TIME_PERIOD = 300000; // 5 minutes

  private final DbStore dbStore;
  private final TargetSelector targetSelector;
  private final MembershipService membershipService;
  private final String id;
  private final Map<Long, Block> blocks;
  private final ServiceManager serviceManager;
  private final BlockingQueue<RambleMessage.Message> messageQueue;
  private final MessageBroadcaster messageBroadcaster;

  public ComputeComplementService(MessageBroadcaster messageBroadcaster, MembershipService membershipService, DbStore dbStore,
                                  BlockingQueue<RambleMessage.Message> messageQueue, String id) {
    this.dbStore = dbStore;
    this.targetSelector = new RandomTargetSelector();
    this.membershipService = membershipService;
    this.messageQueue = messageQueue;
    this.id = id;
    this.blocks = new ConcurrentHashMap<>();
    this.messageBroadcaster = messageBroadcaster;

    FlushBlocksService flushBlocksService = new FlushBlocksService();
    flushBlocksService.addListener(new Listener() {
      @Override
      public void starting() {
        LOG.info("[id = " + id + "] Flush Blocks Service started");
      }

      @Override
      public void terminated(Service.State from) {
        LOG.info("[id = " + id + "] Flush Blocks Service terminated");
      }
    }, ForkJoinPool.commonPool());

    this.serviceManager = new ServiceManager(ImmutableSet.of(flushBlocksService));
  }

  @Override
  public void startUp() {
    this.serviceManager.startAsync();
  }

  @Override
  public void shutDown() {
    this.serviceManager.stopAsync();
  }

  @Override
  public void runOneIteration() throws InterruptedException {
    // Select the target node to run the protocol against
    Optional<RambleMember> target = this.targetSelector.getTarget(this.membershipService.getMembers());

    if (target.isPresent()) {
      // Initialize timestamps to define the range of blocks

      // The timestamp up to which blocks will be synced
      long ts = System.currentTimeMillis();
      long endTimestamp = (long) Math.ceil((double) ts / BLOCK_TIME_PERIOD) * BLOCK_TIME_PERIOD;

      // The timestamp that the block sync iteration will start at
      long lastVerifiedTimestamp = this.dbStore.getLastVerifiedTimestamp(MIN_COUNT_FOR_CONFIRM);
      MutableLong currentTimestamp = new MutableLong(
              lastVerifiedTimestamp > 0 ? lastVerifiedTimestamp : endTimestamp - BLOCK_TIME_PERIOD);

      CountDownLatch latch = new CountDownLatch(1);

      String idAndTarget = "[id = " + this.id + ", target = " + target.get().getAddr() + ":" +
              target.get().getMessageSyncPort() + "]";
      LOG.info(idAndTarget + " Running Compute Complement for block range (" + currentTimestamp + ", " + endTimestamp +
              ")");

      MessageSyncClient messageSyncClient = MessageSyncClientFactory.getMessageSyncClient(this.id,
              target.get().getAddr(),
              target.get().getMessageSyncPort(),
              new ComputeComplementHandler(target.get(), latch, currentTimestamp, endTimestamp));

      messageSyncClient.connect();

      long nextTimestamp = currentTimestamp.longValue() + BLOCK_TIME_PERIOD;
      messageSyncClient.sendRequest(RequestBuilder.buildGetComplementRequest(
              getDigestBlock(currentTimestamp.longValue(), nextTimestamp),
              currentTimestamp.longValue(), nextTimestamp));

      // For now we make this synchronous so that only one iteration of the ComputeComplement protocol can run at once
      // for a given node
      latch.await();
      LOG.info(idAndTarget + " Compute Complement round has completed");
    }
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedRateSchedule(2500, 30000, TimeUnit.MILLISECONDS);
  }

  private Set<byte[]> getDigestBlock(long startTimestamp, long endTimestamp) {
    return this.dbStore.getDigestRange(startTimestamp, endTimestamp);
  }

  private class ComputeComplementHandler implements MessageClientSyncHandler {

    private final RambleMember target;
    private final CountDownLatch latch;
    private final MutableLong currentTimestamp;
    private final long endTimestamp;

    private ComputeComplementHandler(RambleMember target, CountDownLatch latch,
                                     MutableLong currentTimestamp, long endTimestamp) {
      this.target = target;
      this.latch = latch;
      this.currentTimestamp = currentTimestamp;
      this.endTimestamp = endTimestamp;
    }

    @Override
    public void handleResponse(MessageSyncClient messageSyncClient, MessageSyncProtocol.Response response) {
      // Add digests to cache
      blocks.put(this.currentTimestamp.longValue(), new Block(response.getSendMessageDigests()
              .getMessageDigestList()
              .stream()
              .map(ByteString::toByteArray)
              .collect(Collectors.toSet()),
              this.target));

      // Update the current timestamp
      this.currentTimestamp.add(BLOCK_TIME_PERIOD);

      // If the currentTimestamp has exceed the endTimestamp, then end the protocol
      if (this.currentTimestamp.longValue() < this.endTimestamp) {

        long nextTimestamp = this.currentTimestamp.longValue() + BLOCK_TIME_PERIOD;
        messageSyncClient.sendRequest(RequestBuilder.buildGetComplementRequest(
                getDigestBlock(this.currentTimestamp.longValue(), nextTimestamp),
                this.currentTimestamp.longValue(), nextTimestamp));

      } else {
        messageSyncClient.disconnect();
        this.latch.countDown();
      }
    }
  }

  private static class Block {

    private final Set<byte[]> block;
    private final RambleMember source;

    private Block(Set<byte[]> block, RambleMember source) {
      this.block = block;
      this.source = source;
    }

    public Set<byte[]> getBlock() {
      return this.block;
    }

    public RambleMember getSource() {
      return this.source;
    }
  }

  private class FlushBlocksService extends AbstractScheduledService implements Service {

    @Override
    protected void runOneIteration() throws InterruptedException {
      for (Map.Entry<Long, Block> entry : blocks.entrySet()) {

        Block block = entry.getValue();
        long blockTs = entry.getKey();

        if (!block.getBlock().isEmpty()) {
          LOG.info("[id = " + id + ", target = " + block.getSource().getAddr() + ":" +
                  block.getSource().getMessageSyncPort() + "] Running Flush Blocks");

          MessageSyncClient messageSyncClient = MessageSyncClientFactory.getMessageSyncClient(id,
                  block.getSource().getAddr(),
                  block.getSource().getMessageSyncPort(),
                  new FlushBlockHandler(blockTs));

          messageSyncClient.connect();
          messageSyncClient.sendRequest(RequestBuilder.buildGetMessagesRequest(block.getBlock()));
        }
      }
      blocks.clear();
    }

    @Override
    protected Scheduler scheduler() {
      return Scheduler.newFixedRateSchedule(5000, 60000, TimeUnit.MILLISECONDS);
    }
  }

  private final class FlushBlockHandler implements MessageClientSyncHandler {

    private final long blockTs;

    private FlushBlockHandler(long blockTs) {
      this.blockTs = blockTs;
    }

    @Override
    public void handleResponse(MessageSyncClient messageSyncClient, MessageSyncProtocol.Response response) {
      LOG.info("[id = " + id + "] Updating count for block " + this.blockTs);
      dbStore.updateBlockConfirmation(this.blockTs);

      List<RambleMessage.SignedMessage> messages = response.getSendMessage().getMessages()
              .getSignedMessageList();
      if (MessageSigner.verify(messages)) {
        response.getSendMessage().getMessages().getSignedMessageList().forEach(message -> {
          if (!dbStore.exists(message)) {
            LOG.info("[id = " + id + "] Dumping flush blocks message: " + message.getMessage().getMessage());
            dbStore.storeIfNotExists(message);
            try {
              messageBroadcaster.broadcastMessage(message);
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
            try {
              messageQueue.put(message.getMessage());
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          }
        });
      }
      messageSyncClient.disconnect();
    }
  }
}
