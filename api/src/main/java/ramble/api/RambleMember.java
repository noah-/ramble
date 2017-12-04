package ramble.api;

import java.net.URI;

public class RambleMember {

  private final URI uri;
  private final int messageSyncPort;

  public RambleMember(URI uri, int messageSyncPort) {
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
