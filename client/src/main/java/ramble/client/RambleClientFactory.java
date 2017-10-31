package ramble.client;

public class RambleClientFactory {

  public RambleClient getClient() {
    return new RambleClientImpl();
  }
}
