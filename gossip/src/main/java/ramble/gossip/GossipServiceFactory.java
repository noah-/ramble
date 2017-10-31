package ramble.gossip;

import ramble.gossip.apache.ApacheGossipService;
import ramble.gossip.api.GossipService;

import java.io.IOException;
import java.net.URISyntaxException;

public class GossipServiceFactory {

  public GossipService buildGossipService() throws InterruptedException, IOException, URISyntaxException {
    return new ApacheGossipService();
  }
}
