package ramble.gossip.api;


import java.util.List;

public interface GossipService {

  public void gossip(String message);

  public void start();

  public void shutdown();

  public void printMessages();

  public List<String> getPeers();
}
