package ramble.gossip.core;

import ramble.gossip.apache.ApacheGossipService;
import ramble.gossip.api.GossipPeer;
import ramble.gossip.api.GossipService;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;


/**
 * Factory for {@link GossipService}. Currently, there is only one implementation of {@link GossipService}, so this
 * class is very simple. However, having this class around makes it a lot easier to add a new implementation of
 * {@link GossipService}.
 */
public class GossipServiceFactory {

  public static GossipService buildGossipService(URI uri, List<GossipPeer> peers, PublicKey publicKey,
                                                 PrivateKey privateKey)
          throws InterruptedException, IOException, URISyntaxException {
    return new ApacheGossipService(uri, peers, publicKey, privateKey);
  }
}
