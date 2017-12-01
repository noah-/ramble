package ramble.cluster;

import ramble.api.RambleMessage;
import ramble.cluster.crypto.JavaKeyGenerator;
import ramble.crypto.KeyServiceException;
import ramble.db.persistent.PersistentDbStore;
import ramble.gossip.api.GossipPeer;
import ramble.gossip.api.GossipService;
import ramble.gossip.core.GossipServiceFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


class RambleGossipCluster {

  private final List<URI> nodes;
  private final List<URI> seeds;

  private RambleGossipCluster(List<URI> nodes, List<URI> seeds) {
    this.nodes = nodes;
    this.seeds = seeds;
  }

  private List<GossipService> run() throws InterruptedException, IOException, URISyntaxException, NoSuchAlgorithmException, KeyServiceException {

    // Create and store all keys
//    KeyStoreService keyStoreService = KeyStoreServiceFactory.getKeyStoreService();
//    JavaKeyGenerator javaKeyGenerator = new JavaKeyGenerator(1024);
//    javaKeyGenerator.createKeys();

//    for (URI node : nodes) {
//      keyStoreService.deletePublicKey(URIUtils.uriToId(node));
//      keyStoreService.deletePrivateKey(URIUtils.uriToId(node));
//      keyStoreService.putPublicKey(URIUtils.uriToId(node), javaKeyGenerator.getPublicKey());
//      keyStoreService.putPrivateKey(URIUtils.uriToId(node), javaKeyGenerator.getPrivateKey());
//    }

    List<GossipService> clients = createCluster();
    clients.forEach(GossipService::start);
    for (int i = 0; i < 10; i++) {
      Thread.sleep(10000);

      clients.forEach(gossipService -> gossipService.gossip("Hello World"));

      System.out.println("Live members: ");
      clients.forEach(client -> System.out.println(Arrays.toString(client.getConnectedPeers().toArray())));
    }
    return clients;
  }

  private List<GossipService> createCluster()throws InterruptedException, IOException, URISyntaxException, NoSuchAlgorithmException {
    List<GossipPeer> startupMembers = this.seeds.stream().map(GossipPeer::new).collect(Collectors.toList());

    JavaKeyGenerator javaKeyGenerator = new JavaKeyGenerator(1024);
    javaKeyGenerator.createKeys();

    List<GossipService> clients = new ArrayList<>();
    for (URI node : this.nodes) {
      clients.add(GossipServiceFactory.buildGossipService(node, startupMembers, javaKeyGenerator.getPublicKey(), javaKeyGenerator.getPrivateKey()));
    }
    return clients;
  }

  public static void main(String args[]) throws InterruptedException, IOException, URISyntaxException,
          NoSuchAlgorithmException, KeyServiceException, SQLException {

    PersistentDbStore.runInitializeScripts();

    List<URI> nodes = new ArrayList<>();
    nodes.add(URI.create("udp://127.0.0.1:5000"));
    nodes.add(URI.create("udp://127.0.0.1:5001"));
    nodes.add(URI.create("udp://127.0.0.1:5002"));
    nodes.add(URI.create("udp://127.0.0.1:5003"));
    nodes.add(URI.create("udp://127.0.0.1:5004"));

    List<URI> seeds = new ArrayList<>();
    seeds.add(URI.create("udp://127.0.0.1:5000"));
    seeds.add(URI.create("udp://127.0.0.1:5001"));
    seeds.add(URI.create("udp://127.0.0.1:5002"));

    List<GossipService> clients = new RambleGossipCluster(nodes, seeds).run();

    while (true) {
      for (GossipService client : clients) {
        RambleMessage.Message message;
        while ((message = client.subscribe().take()) != null) {
          System.out.println("Got message " + message.getMessage());
        }
      }
    }
  }
}
