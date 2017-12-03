package ramble.gossip.api;

import java.util.List;


/**
 * A service that provides membership lists via a Gossip protocol.
 */
public interface GossipService {

  void start();

  void shutdown();

  List<GossipMember> getMembers();
}
