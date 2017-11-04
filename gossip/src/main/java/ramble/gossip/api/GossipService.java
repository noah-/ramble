package ramble.gossip.api;

import java.util.List;
import java.util.concurrent.BlockingQueue;


/**
 * A service that provides Gossip capabilities. Messages can be sent via Gossip, and users can listen for any incoming
 * Gossip messages.
 */
public interface GossipService {

  void gossip(String message);

  void start();

  void shutdown();

  BlockingQueue<IncomingMessage> subscribe();

  List<String> getConnectedPeers();

  String getURI();
}
