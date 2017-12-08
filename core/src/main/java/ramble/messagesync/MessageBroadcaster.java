package ramble.messagesync;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.Service;
import org.apache.log4j.Logger;
import ramble.api.MembershipService;
import ramble.api.RambleMember;
import ramble.api.RambleMessage;
import ramble.messagesync.api.MessageSyncClient;
import ramble.messagesync.api.TargetSelector;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class MessageBroadcaster extends AbstractExecutionThreadService implements Service {

  private static final Logger LOG = Logger.getLogger(MessageBroadcaster.class);

  private final BlockingQueue<RambleMessage.SignedMessage> messages;
  private final MembershipService membershipService;
  private final TargetSelector targetSelector;
  private final String id;

  public MessageBroadcaster(String id, MembershipService membershipService) {
    this.id = id;
    this.messages = new ArrayBlockingQueue<>(1024);
    this.membershipService = membershipService;
    this.targetSelector = new RandomTargetSelector();
  }

  public void broadcastMessage(RambleMessage.SignedMessage signedMessage) throws InterruptedException {
    this.messages.put(signedMessage);
  }

  @Override
  protected void run() throws InterruptedException {
    while (isRunning()) {
      Set<RambleMessage.SignedMessage> broadcastMessages = new HashSet<>();
      broadcastMessages.add(this.messages.take());

      RambleMember target = this.targetSelector.getTarget(this.membershipService.getMembers());

      LOG.info("Broadcasting messages from id = " + this.id + " to target " + target.getAddr());

      MessageSyncClient messageSyncClient = MessageSyncClientFactory.getMessageSyncClient(target.getAddr(),
              target.getMessageSyncPort(), new StorageMessageSyncHandler(new LinkedBlockingQueue<>(), this.id));
      messageSyncClient.connect();
      messageSyncClient.sendRequest(RequestBuilder.buildBroadcastMessagesRequest(broadcastMessages));
    }
  }
}
