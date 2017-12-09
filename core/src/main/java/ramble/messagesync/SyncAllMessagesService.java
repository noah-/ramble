package ramble.messagesync;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.Service;
import org.apache.log4j.Logger;
import ramble.api.MembershipService;
import ramble.api.RambleMember;
import ramble.api.RambleMessage;
import ramble.core.RambleImpl;
import ramble.messagesync.api.MessageSyncClient;
import ramble.messagesync.api.TargetSelector;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;


public class SyncAllMessagesService extends AbstractScheduledService implements Service {

  private static final Logger LOG = Logger.getLogger(RambleImpl.class);

  private final MembershipService gossipService;
  private final String id;
  private final TargetSelector targetSelector;
  private final BlockingQueue<RambleMessage.Message> messageQueue;

  public SyncAllMessagesService(MembershipService gossipService, String id,
                                BlockingQueue<RambleMessage.Message> messageQueue) {
    this.gossipService = gossipService;
    this.id = id;
    this.targetSelector = new RandomTargetSelector();
    this.messageQueue = messageQueue;
  }

  @Override
  protected void runOneIteration() throws InterruptedException {
    LOG.info("[id = " + this.id + "] Running message sync protocol");

    Set<RambleMember> peers = this.gossipService.getMembers();
    if (!peers.isEmpty()) {
      RambleMember target = this.targetSelector.getTarget(peers);

      MessageSyncClient client = MessageSyncClientFactory.getMessageSyncClient(target.getAddr(),
              target.getMessageSyncPort(), new StorageMessageSyncHandler(this.messageQueue, this.id));

      // May need to add an explicit disconnect here in case there is an error while sending the request, but Netty may
      // handle it internally so its ok for now
      client.connect();
      client.sendRequest(RequestBuilder.buildGetAllMessagesRequest());
    }
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedRateSchedule(1500, 5000, TimeUnit.MILLISECONDS);
  }
}
