package ramble.gossip.api;

import java.net.URI;
import java.util.List;


/**
 * A service that provides Gossip capabilities. Messages can be sent via Gossip, and users can listen for any incoming
 * Gossip messages.
 */
public interface GossipService {

  void gossip(String message);

  void start();

  void shutdown();

  List<URI> getConnectedPeers();

  String getURI();
}
