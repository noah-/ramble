package ramble.cluster;

import com.google.common.base.Splitter;
import com.google.common.io.Files;
import org.testng.Assert;
import org.testng.annotations.Test;
import ramble.api.Ramble;
import ramble.api.RambleMessage;
import ramble.core.MessageBuilder;
import ramble.core.RambleImpl;
import ramble.crypto.JavaKeyGenerator;
import ramble.db.h2.H2DbStore;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Simple integration tests that launches several {@link Ramble} instances and links them together. Each instance then
 * posts several random messages. The test validates that each instance got all messages from all other instances in
 * the cluster
 */
@Test
public class RambleCluster {

  @Test
  public void rambleClusterTest() throws InterruptedException, IOException, NoSuchAlgorithmException {

    // Logger.getLogger("io.netty").setLevel(Level.OFF);

    String addr = InetAddress.getLocalHost().getHostAddress();

    // Creates all nodes
    List<URI> nodes = new ArrayList<>();
    nodes.add(URI.create("udp://" + addr + ":5000"));
    nodes.add(URI.create("udp://" + addr + ":5001"));
    nodes.add(URI.create("udp://" + addr + ":5002"));
    nodes.add(URI.create("udp://" + addr + ":5003"));
    nodes.add(URI.create("udp://" + addr + ":5004"));

    // Create initial seeds
    List<URI> seeds = new ArrayList<>();
    seeds.add(URI.create("udp://" + addr + ":5000"));
    seeds.add(URI.create("udp://" + addr + ":5001"));
    seeds.add(URI.create("udp://" + addr + ":5002"));

    // Randomly select text from lorem-ipsum file
    List<String> loremIpsum = new ArrayList<>();
    List<String> file = Files.readLines(new File("src/test/resources/lorem-ipsum.txt"),
            Charset.defaultCharset());
    Splitter splitter = Splitter.on(" ").omitEmptyStrings().trimResults();
    file.forEach(f -> loremIpsum.addAll(splitter.splitToList(f)));

    // Generate keys for each instance
    JavaKeyGenerator javaKeyGenerator = new JavaKeyGenerator(1024);
    javaKeyGenerator.createKeys();

    // Create each instance
    List<Ramble> clients = new ArrayList<>();
    for (URI node : nodes) {
      clients.add(
              new RambleImpl(seeds, javaKeyGenerator.getPublicKey(), javaKeyGenerator.getPrivateKey(), node.getPort(),
                      node.getPort() + 1000));
    }

    // Setup the db for each instance and add some dummy rows
    clients.forEach(client -> {
      try {
        H2DbStore db = H2DbStore.getOrCreateStore(client.getId());
        db.runInitializeScripts();

        RambleMessage.SignedMessage msg1 = MessageBuilder.buildSignedMessage(client.getId(),
                getRandomLoremIpsum(loremIpsum), client.getPublicKey(), client.getPrivateKey());
        RambleMessage.SignedMessage msg2 = MessageBuilder.buildSignedMessage(client.getId(),
                getRandomLoremIpsum(loremIpsum), client.getPublicKey(), client.getPrivateKey());
        RambleMessage.SignedMessage msg3 = MessageBuilder.buildSignedMessage(client.getId(),
                getRandomLoremIpsum(loremIpsum), client.getPublicKey(), client.getPrivateKey());

        db.store(msg1);
        db.store(msg2);
        db.store(msg3);
      } catch (SQLException e) {
        e.printStackTrace();
      }
    });

    // Start each instance
    clients.forEach(Ramble::start);

    // Wait for them to discover each other
    Thread.sleep(5000);

    // Validate each member has seen all other members
    clients.forEach(mem -> Assert.assertEquals(mem.getMembers().size(), 4));

    // Post messages every 5 seconds
    for (int i = 0; i < 3; i++) {
      clients.forEach(gossipService -> gossipService.post(getRandomLoremIpsum(loremIpsum)));

      Thread.sleep(5000);
    }

    // Wait for everything to converge
    Thread.sleep(60000);

    // Shutdown all instances
    clients.forEach(Ramble::shutdown);

    // Make sure each instance has 15 messages in its db
    clients.forEach(client -> Assert.assertEquals(client.getAllMessages().size(), 30));
  }

  private String getRandomLoremIpsum(List<String> loremIpsum) {
    Collections.shuffle(loremIpsum);
    return String.join(" ", loremIpsum.subList(0, 5));
  }
}
