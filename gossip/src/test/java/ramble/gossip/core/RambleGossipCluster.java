package ramble.gossip.core;

import ramble.gossip.api.GossipPeer;
import ramble.gossip.api.GossipService;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Similar to {@link ramble.gossip.apache.ApacheGossipCluster}, but for {@link GossipService}s instead.
 */
public class RambleGossipCluster {

  private void run() throws InterruptedException, IOException, URISyntaxException {
    List<GossipService> clients = createCluster();
    clients.forEach(GossipService::start);
    for (int i = 0; i < 10; i++) {
      Thread.sleep(10000);

      System.out.println("Live members: ");
      clients.forEach(client -> System.out.println(Arrays.toString(client.getConnectedPeers().toArray())));
    }
  }

  List<GossipService> createCluster() throws InterruptedException, IOException, URISyntaxException {
    int seedNodes = 3;
    List<GossipPeer> startupMembers = new ArrayList<>();
    for (int i = 1; i < seedNodes + 1; ++i) {
      URI uri = URI.create("udp://" + "127.0.0.1" + ":" + (50000 + i));
      startupMembers.add(new GossipPeer(uri));
    }

    List<GossipService> clients = new ArrayList<>();
    int clusterMembers = 5;
    for (int i = 1; i < clusterMembers + 1; ++i) {
      URI uri = URI.create("udp://" + "127.0.0.1" + ":" + (50000 + i));
      clients.add(GossipServiceFactory.buildGossipService(uri, startupMembers));
    }
    return clients;
  }

  public static void main(String args[]) throws InterruptedException, IOException, URISyntaxException {
    new RambleGossipCluster().run();
  }
}
