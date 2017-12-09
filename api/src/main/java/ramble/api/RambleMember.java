package ramble.api;

public class RambleMember {

  private final String addr;
  private final int gossipPort;
  private final int messageSyncPort;

  public RambleMember(String addr, int gossipPort, int messageSyncPort) {
    this.addr = addr;
    this.gossipPort = gossipPort;
    this.messageSyncPort = messageSyncPort;
  }

  public String getAddr() {
    return addr;
  }

  public int getGossipPort() {
    return gossipPort;
  }

  public int getMessageSyncPort() {
    return messageSyncPort;
  }
}
