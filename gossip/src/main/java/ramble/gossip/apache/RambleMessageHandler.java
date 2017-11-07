package ramble.gossip.apache;

import org.apache.gossip.manager.GossipCore;
import org.apache.gossip.manager.GossipManager;
import org.apache.gossip.manager.handlers.MessageHandler;
import org.apache.gossip.model.Base;


public class RambleMessageHandler implements MessageHandler {

  // Need to plug this in somewhere, it basically means you got a message and should add it to the BlockingQueue in
  // ApacheGossipService

  @Override
  public boolean invoke(GossipCore gossipCore, GossipManager gossipManager, Base base) {
    return false;
  }
}
