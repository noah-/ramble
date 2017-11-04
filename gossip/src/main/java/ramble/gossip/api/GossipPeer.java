package ramble.gossip.api;

import java.net.URI;


/**
 * A peer on the Gossip network
 */
public class GossipPeer {

  private final URI peerURI;

  public GossipPeer(URI peerURI) {
    this.peerURI = peerURI;
  }

  public URI getPeerURI() {
    return this.peerURI;
  }
}
