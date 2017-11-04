package ramble.gossip.apache;

import org.apache.gossip.GossipSettings;
import org.apache.gossip.Member;
import org.apache.gossip.RemoteMember;
import org.apache.gossip.manager.GossipManager;
import org.apache.gossip.manager.GossipManagerBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * Creates a small Gossip cluster locally and prints out all live and dead nodes. Mostly for testing purposes.
 */
public class ApacheGossipCluster {

  private void run() throws InterruptedException, UnknownHostException, URISyntaxException {
    List<GossipManager> clients = createCluster();
    clients.forEach(GossipManager::init);
    for (int i = 0; i < 10; i++) {
      Thread.sleep(10000);

      System.out.println("Live members: ");
      clients.forEach(client -> System.out.println(client.getLiveMembers()));

      System.out.println("Dead members: ");
      clients.forEach(client -> System.out.println(client.getDeadMembers()));
    }
  }

  private List<GossipManager> createCluster() throws UnknownHostException, InterruptedException, URISyntaxException {
    GossipSettings settings = new GossipSettings();

    int seedNodes = 3;
    List<Member> startupMembers = new ArrayList<>();
    for (int i = 1; i < seedNodes + 1; ++i) {
      URI uri = new URI("udp://" + "127.0.0.1" + ":" + (50000 + i));
      startupMembers.add(new RemoteMember("test", uri, uri.toString()));
    }

    List<GossipManager> clients = new ArrayList<>();
    int clusterMembers = 5;
    for (int i = 1; i < clusterMembers + 1; ++i) {
      URI uri = new URI("udp://" + "127.0.0.1" + ":" + (50000 + i));
      clients.add(GossipManagerBuilder.newBuilder()
              .cluster("test")
              .uri(uri)
              .id(uri.toString())
              .gossipMembers(startupMembers)
              .gossipSettings(settings)
              .build());
    }
    return clients;
  }

  public static void main(String args[]) throws InterruptedException, UnknownHostException, URISyntaxException {
    new ApacheGossipCluster().run();
  }
}
