package ramble.gossip.api;

import java.util.List;
import java.util.concurrent.BlockingQueue;


public interface GossipService {

  void gossip(String message);

  void start();

  void shutdown();

  BlockingQueue<IncomingMessage> subscribe();

  List<String> getConnectedPeers();

  String getURI();
}
