package ramble.gossip.apache;

/**
 * Mostly for testing purposes
 */
public class ApacheGossipCluster {

//  private List<GossipService> createCluster() throws UnknownHostException, InterruptedException, URISyntaxException {
//    GossipSettings settings = new GossipSettings();
//    int seedNodes = 3;
//    List<GossipMember> startupMembers = new ArrayList<>();
//    for (int i = 1; i < seedNodes + 1; ++i) {
//      URI uri = new URI("udp://" + "127.0.0.1" + ":" + (50000 + i));
//      startupMembers.add(new RemoteGossipMember("test", uri, i + ""));
//    }
//
//    List<GossipService> clients = new ArrayList<>();
//    int clusterMembers = 5;
//    for (int i = 1; i < clusterMembers + 1; ++i) {
//      URI uri = new URI("udp://" + "127.0.0.1" + ":" + (50000 + i));
//      clients.add(new GossipService("test", uri, i + "", Maps.newHashMap(), startupMembers, settings, (a, b) -> {}, new MetricRegistry()));
//    }
//    return clients;
//  }
//
//  public static void main(String args[]) throws InterruptedException, UnknownHostException, URISyntaxException {
//    List<GossipService> clients = new ApacheGossipCluster().createCluster();
//    clients.forEach(GossipService::start);
//    Thread.sleep(10000);
//    clients.forEach(client -> System.out.print(client.getGossipManager().getLiveMembers()));
//  }
}
