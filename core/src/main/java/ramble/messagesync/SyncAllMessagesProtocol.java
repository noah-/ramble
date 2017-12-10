package ramble.messagesync;

import org.apache.log4j.Logger;
import ramble.api.MembershipService;
import ramble.api.RambleMember;
import ramble.api.RambleMessage;
import ramble.core.RambleImpl;
import ramble.messagesync.api.MessageSyncClient;
import ramble.messagesync.api.TargetSelector;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;


public class SyncAllMessagesProtocol {

  private static final Logger LOG = Logger.getLogger(RambleImpl.class);

  private final MembershipService gossipService;
  private final String id;
  private final TargetSelector targetSelector;
  private final BlockingQueue<RambleMessage.Message> messageQueue;

  public SyncAllMessagesProtocol(MembershipService gossipService, String id,
                                BlockingQueue<RambleMessage.Message> messageQueue) {
    this.gossipService = gossipService;
    this.id = id;
    this.targetSelector = new RandomTargetSelector();
    this.messageQueue = messageQueue;
  }

  public void run() throws InterruptedException {
    LOG.info("[id = " + this.id + "] Running message sync protocol");

    Set<RambleMember> peers = this.gossipService.getMembers();
    Optional<RambleMember> target = this.targetSelector.getTarget(peers);

    if (target.isPresent()) {
      MessageSyncClient client = MessageSyncClientFactory.getMessageSyncClient(target.get().getAddr(),
              target.get().getMessageSyncPort(), new MessageQueueMessageClientSyncHandler(this.messageQueue, this.id));

      // May need to add an explicit disconnect here in case there is an error while sending the request, but Netty may
      // handle it internally so its ok for now
      client.connect();
      client.sendRequest(RequestBuilder.buildGetAllMessagesRequest());
    }
  }
}
