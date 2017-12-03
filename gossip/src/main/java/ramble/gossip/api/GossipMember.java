package ramble.gossip.api;

import java.net.URI;

public class GossipMember {

  private final URI uri;
  private final int messageSyncPort;

  public GossipMember(URI uri, int messageSyncPort) {
    this.uri = uri;
    this.messageSyncPort = messageSyncPort;
  }

  public int getMessageSyncPort() {
    return messageSyncPort;
  }

  public URI getUri() {
    return uri;
  }
}
