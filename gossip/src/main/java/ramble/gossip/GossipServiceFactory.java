package ramble.gossip;

import ramble.gossip.apache.ApacheGossipService;
import ramble.gossip.api.GossipPeer;
import ramble.gossip.api.GossipService;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;


public class GossipServiceFactory {

  public static GossipService buildGossipService(URI uri, List<GossipPeer> peers)
          throws InterruptedException, IOException, URISyntaxException {
    return new ApacheGossipService(uri, peers);
  }
}
