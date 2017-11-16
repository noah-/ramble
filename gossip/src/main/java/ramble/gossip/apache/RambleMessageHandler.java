package ramble.gossip.apache;

import org.apache.gossip.manager.GossipCore;
import org.apache.gossip.manager.GossipManager;
import org.apache.gossip.manager.handlers.MessageHandler;
import org.apache.gossip.model.Base;
import org.apache.gossip.model.RambleBulkMessage;
import ramble.api.RambleMessage;

import java.util.concurrent.BlockingQueue;


public class RambleMessageHandler implements MessageHandler {

  private final BlockingQueue<RambleMessage.Message> messageQueue;

  public RambleMessageHandler(BlockingQueue<RambleMessage.Message> messageQueue) {
    this.messageQueue = messageQueue;
  }

  @Override
  public boolean invoke(GossipCore gossipCore, GossipManager gossipManager, Base base) {
    RambleBulkMessage message = (RambleBulkMessage) base;
    for (RambleMessage.Message rambleMessage : message.getBulkMessage().getMessageList()) {
      try {
        messageQueue.put(rambleMessage);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    return false;
  }
}
