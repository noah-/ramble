package ramble.gossip.core;

import ramble.gossip.api.GossipService;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;


/**
 * Similar to {@link RambleGossipCluster} except instead of just starting a Gossip cluster and printing live nodes, it
 * actually sends messages between the nodes.
 */
public class RambleCluster {

  private void run() throws InterruptedException, IOException, URISyntaxException {
    RambleGossipCluster gossipCluster = new RambleGossipCluster();
    List<GossipService> gossipServices = gossipCluster.createCluster();

    // TODO
  }
}
