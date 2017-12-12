package ramble.messagesync;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.Service;
import org.apache.log4j.Logger;
import ramble.api.MembershipService;
import ramble.api.RambleMember;
import ramble.api.RambleMessage;
import ramble.messagesync.api.MessageSyncClient;
import ramble.messagesync.api.TargetSelector;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


public class MessageBroadcaster extends AbstractExecutionThreadService implements Service {

  private static final Logger LOG = Logger.getLogger(MessageBroadcaster.class);

  private final BlockingQueue<RambleMessage.SignedMessage> messages;
  private final MembershipService membershipService;
  private final TargetSelector targetSelector;
  private final String id;
  private final int fanout;
  private final PublicKey publicKey;

  public MessageBroadcaster(String id, MembershipService membershipService, PublicKey publicKey, int fanout) {
    this.id = id;
    this.messages = new ArrayBlockingQueue<>(1024);
    this.membershipService = membershipService;
    this.fanout = fanout;
    this.publicKey = publicKey;
    this.targetSelector = new RandomTargetSelector();
  }

  public void broadcastMessage(RambleMessage.SignedMessage signedMessage) throws InterruptedException {
    this.messages.put(signedMessage);
  }

  @Override
  public void startUp() {
    LOG.info("[id = " + this.id + "] Starting broadcast service");
  }

  @Override
  protected void run() throws InterruptedException {
    while (isRunning()) {
      Set<RambleMessage.SignedMessage> broadcastMessages = new HashSet<>();
      broadcastMessages.add(this.messages.take());
      this.messages.drainTo(broadcastMessages);

      Set<RambleMember> targets = this.targetSelector.getTargets(this.membershipService.getMembers(), this.fanout);

      for (RambleMember target : targets) {
        LOG.info("[id = " + this.id + "] Broadcasting messages: " + Arrays.toString(broadcastMessages
                .stream()
                .map(msg -> msg.getMessage().getMessage()).toArray()) + " to target " + target.getAddr() + ":" +
                target.getMessageSyncPort());

        MessageSyncClient messageSyncClient = MessageSyncClientFactory.getMessageSyncClient(this.id, target.getAddr(),
                target.getMessageSyncPort(), new AckMessageClientSyncHandler());
        messageSyncClient.connect();
        messageSyncClient.sendRequest(RequestBuilder.buildBroadcastMessagesRequest(broadcastMessages, this.publicKey));
      }
    }
  }

  @Override
  public void shutDown() {
    LOG.info("[id = " + this.id + "] Shutting down broadcast service");
  }
}
