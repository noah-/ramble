package ramble.messagesync;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.protobuf.ByteString;
import org.apache.commons.lang.mutable.MutableLong;
import org.apache.log4j.Logger;
import ramble.api.MembershipService;
import ramble.api.RambleMember;
import ramble.api.RambleMessage;
import ramble.crypto.MessageSigner;
import ramble.db.api.DbStore;
import ramble.messagesync.api.MessageSyncClient;
import ramble.messagesync.api.TargetSelector;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * An anti-entropy protocol iteratively syncs its messages with a target random node. This is done by dividing all local
 * messages into logical blocks that are bound by a time window. THe initiating starts at the most recent block, and
 * sends all messages digests in the block to the target node. The target node responds with a list of messages that the
 * source node is missing from the block in question. The source node then adds all these messages to its local db, and
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

  public ComputeComplementService(MembershipService membershipService, DbStore dbStore, String id) {
    this.dbStore = dbStore;
    this.targetSelector = new RandomTargetSelector();
    this.membershipService = membershipService;
    this.id = id;
    this.blocks = new ConcurrentHashMap<>();
    this.serviceManager = new ServiceManager(ImmutableSet.of(new FlushBlocksService(this.id, this.dbStore, this.blocks)));
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
      long lastVerifiedTimestamp = this.dbStore.getLastVerifiedTimestamp(
              MIN_COUNT_FOR_CONFIRM); // how does this BLOCKCONF table get updated
      MutableLong currentTimestamp = new MutableLong(
              lastVerifiedTimestamp > 0 ? lastVerifiedTimestamp : endTimestamp - BLOCK_TIME_PERIOD);

      CountDownLatch latch = new CountDownLatch(1);

      String idAndTarget = "[id = " + this.id + ", target = " + target.get().getAddr() + ":" +
              target.get().getMessageSyncPort() + "]";
      LOG.info(idAndTarget + " Compute Complement with currentTs = " + currentTimestamp + " endTs = " + endTimestamp);

      MessageSyncClient messageSyncClient = MessageSyncClientFactory.getMessageSyncClient(
              target.get().getAddr(),
              target.get().getMessageSyncPort(),
              (myMessageSyncClient, response) -> {

                // Add digests to cache
                this.blocks.put(currentTimestamp.longValue(), new Block(response.getSendMessageDigests()
                        .getMessageDigestList()
                        .stream()
                        .map(ByteString::toByteArray)
                        .collect(Collectors.toSet()),
                        target.get()));

                // Update the current timestamp
                currentTimestamp.add(BLOCK_TIME_PERIOD);

                // If the currentTimestamp has exceed the endTimestamp, then end the protocol
                if (currentTimestamp.longValue() < endTimestamp) {
                  myMessageSyncClient.sendRequest(RequestBuilder.buildGetComplementRequest(
                          getDigestBlock(currentTimestamp.longValue(),
                                  currentTimestamp.longValue() + BLOCK_TIME_PERIOD),
                          currentTimestamp.longValue(),
                          currentTimestamp.longValue() + BLOCK_TIME_PERIOD));
                } else {
                  LOG.info(idAndTarget + " Compute Complement disconnecting client");
                  myMessageSyncClient.disconnect();
                  latch.countDown();
                }
              });

      messageSyncClient.connect();
      messageSyncClient.sendRequest(RequestBuilder.buildGetComplementRequest(
              getDigestBlock(currentTimestamp.longValue(), currentTimestamp.longValue() + BLOCK_TIME_PERIOD),
              currentTimestamp.longValue(),
              currentTimestamp.longValue() + BLOCK_TIME_PERIOD));

      // For now we make this synchronous so that only one iteration of the ComputeComplement protocol can run at once
      // for a given node
      latch.await();
      LOG.info(idAndTarget + " Compute Complement has completed");
    }
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedRateSchedule(1500, 2500, TimeUnit.MILLISECONDS);
  }

  private Set<byte[]> getDigestBlock(long startTimestamp, long endTimestamp) {
    return this.dbStore.getDigestRange(startTimestamp, endTimestamp);
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

  private static class FlushBlocksService extends AbstractScheduledService implements Service {

    private final String id;
    private final DbStore dbStore;
    private final Map<Long, Block> blocks;

    private FlushBlocksService(String id, DbStore dbStore, Map<Long, Block> blocks) {
      this.id = id;
      this.dbStore = dbStore;
      this.blocks = blocks;
    }

    @Override
    protected void runOneIteration() throws InterruptedException {
      for (Block block : this.blocks.values()) {

        if (!block.getBlock().isEmpty()) {
          LOG.info("[id = " + id + ", target = " + block.getSource().getAddr() + ":" +
                  block.getSource().getMessageSyncPort() + "] Running Flush Blocks");

          MessageSyncClient messageSyncClient = MessageSyncClientFactory.getMessageSyncClient(
                  block.getSource().getAddr(),
                  block.getSource().getMessageSyncPort(),
                  (myMessageSyncClient, response) -> {
                    List<RambleMessage.SignedMessage> messages = response.getSendMessage().getMessages()
                            .getSignedMessageList();
                    if (MessageSigner.verify(messages)) {
                      response.getSendMessage().getMessages().getSignedMessageList().forEach(
                              this.dbStore::storeIfNotExists);
                    }
                    myMessageSyncClient.disconnect();
                  });

          messageSyncClient.connect();
          messageSyncClient.sendRequest(RequestBuilder.buildGetMessagesRequest(block.getBlock()));
        }
      }
      this.blocks.clear();
    }

    @Override
    protected Scheduler scheduler() {
      return Scheduler.newFixedRateSchedule(1500, 10000, TimeUnit.MILLISECONDS);
    }
  }
}
