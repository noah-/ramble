package ramble.cluster;

import com.google.common.base.Splitter;
import com.google.common.io.Files;
import ramble.api.Ramble;
import ramble.cluster.crypto.JavaKeyGenerator;
import ramble.core.RambleImpl;
import ramble.crypto.KeyServiceException;
import ramble.crypto.URIUtils;
import ramble.db.persistent.PersistentDbStore;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


class RambleCluster {

  private final List<URI> nodes;
  private final List<URI> seeds;

  private List<String> loremIpsum = new ArrayList<>();

  private RambleCluster(List<URI> nodes, List<URI> seeds) throws IOException {
    this.nodes = nodes;
    this.seeds = seeds;
    List<String> file = Files.readLines(new File("cluster/src/test/resources/lorem-ipsum.txt"), Charset.defaultCharset());
    Splitter splitter = Splitter.on(" ").omitEmptyStrings().trimResults();
    file.forEach(f -> this.loremIpsum.addAll(splitter.splitToList(f)));
  }

  // TODO: each client needs to store data in a separate database?
  private List<Ramble> run() throws InterruptedException, IOException, URISyntaxException, NoSuchAlgorithmException {

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

    List<Ramble> clients = createCluster();
    clients.forEach(Ramble::start);
    for (int i = 0; i < 3; i++) {
      Thread.sleep(10000);

      clients.forEach(gossipService -> gossipService.post(getRandomLoremIpsum()));
    }
    return clients;
  }

  private List<Ramble> createCluster() throws InterruptedException, IOException, URISyntaxException, NoSuchAlgorithmException {

    JavaKeyGenerator javaKeyGenerator = new JavaKeyGenerator(1024);
    javaKeyGenerator.createKeys();

    List<Ramble> clients = new ArrayList<>();
    for (URI node : this.nodes) {
      clients.add(new RambleImpl(node, this.seeds, javaKeyGenerator.getPublicKey(), javaKeyGenerator.getPrivateKey(),
              node.getPort() + 1000));
    }
    return clients;
  }

  public static void main(String args[]) throws InterruptedException, IOException, URISyntaxException,
          NoSuchAlgorithmException, KeyServiceException, SQLException {

    List<URI> nodes = new ArrayList<>();
    nodes.add(URI.create("udp://127.0.0.1:5000"));
    nodes.add(URI.create("udp://127.0.0.1:5001"));
    nodes.add(URI.create("udp://127.0.0.1:5002"));
    nodes.add(URI.create("udp://127.0.0.1:5003"));
    nodes.add(URI.create("udp://127.0.0.1:5004"));

    nodes.forEach(uri -> {
      try {
        PersistentDbStore.getOrCreateStore(URIUtils.uriToId(uri)).runInitializeScripts();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    });

    List<URI> seeds = new ArrayList<>();
    seeds.add(URI.create("udp://127.0.0.1:5000"));
    seeds.add(URI.create("udp://127.0.0.1:5001"));
    seeds.add(URI.create("udp://127.0.0.1:5002"));

    new RambleCluster(nodes, seeds).run();
  }

  private String getRandomLoremIpsum() {
    Collections.shuffle(this.loremIpsum);
//    Random random = new Random();
    return String.join(" ", this.loremIpsum.subList(0, 5));
  }
}
