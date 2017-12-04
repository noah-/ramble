package ramble.gossip.apache;

import org.apache.commons.io.FileUtils;
import org.apache.gossip.GossipSettings;
import org.apache.gossip.Member;
import org.apache.gossip.RemoteMember;
import org.apache.gossip.manager.GossipManager;
import org.apache.gossip.manager.GossipManagerBuilder;
import org.apache.log4j.Logger;

import ramble.crypto.URIUtils;
import ramble.api.RambleMember;
import ramble.gossip.api.GossipPeer;
import ramble.api.MembershipService;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Provides membership lists for a Ramble Cluster using Apache Gossip
 */
public class ApacheGossipMembershipService implements MembershipService {

  private static final String GOSSIP_CLUSTER_NAME = "ramble";
  private static final String MESSAGE_SYNC_PORT_KEY = "ramble.message-sync.port";
  private static final String KEY_STORE_FOLDER_NAME = "ramble-key-store";
  private static final Logger LOG = Logger.getLogger(ApacheGossipMembershipService.class);

  private final GossipManager gossipManager;

  @SuppressWarnings("unchecked")
  public ApacheGossipMembershipService(List<GossipPeer> peers, PublicKey publicKey, PrivateKey privateKey, int gossipPort,
                                       int messageSyncPort, String id) throws IOException {

    List<Member> gossipMembers = peers.stream()
            .map(peer -> new RemoteMember(GOSSIP_CLUSTER_NAME, peer.getPeerURI(), URIUtils.uriToId(peer.getPeerURI())))
            .collect(Collectors.toList());

    URI uri = URI.create("udp://127.0.0.1:" + gossipPort);

    // There seems to be a bug in Apache Gossip where it only accepts ids of a certain form, so we can't use the id we
    // use everywhere else, we have to use one in the below format
    String gossipId = uriToGossipId(uri);

    String keyStoreFolder = setupKeyStore(publicKey, privateKey, id, gossipId);

    GossipSettings gossipSettings = new GossipSettings();
    gossipSettings.setDistribution("exponential");
    gossipSettings.setPathToKeyStore(keyStoreFolder);

    Map<String, String> properties = new HashMap<>();
    properties.put(MESSAGE_SYNC_PORT_KEY, Integer.toString(messageSyncPort));

    this.gossipManager = GossipManagerBuilder.newBuilder()
            .cluster(GOSSIP_CLUSTER_NAME)
            .uri(uri)
            .properties(properties)
            .id(gossipId)
            .gossipMembers(gossipMembers)
            .gossipSettings(gossipSettings)
            .listener(((member, gossipState) -> LOG.info("Member " + member + " reported status " + gossipState)))
            .build();
  }

  /**
   * In order to make sure Apache Gossip signs all messages, the public key and private key need to be written to a
   * file on the local fs.
   */
  private String setupKeyStore(PublicKey publicKey, PrivateKey privateKey, String id,
                               String gossipId) throws IOException {
    Path keyFolder = Paths.get(System.getProperty("user.home"), KEY_STORE_FOLDER_NAME, id);
    FileUtils.deleteDirectory(new File(keyFolder.toString()));
    Files.createDirectories(keyFolder);
    Files.write(Paths.get(keyFolder.toString(), gossipId + ".pub"), publicKey.getEncoded(),
            StandardOpenOption.CREATE_NEW);
    Files.write(Paths.get(keyFolder.toString(), gossipId), privateKey.getEncoded(), StandardOpenOption.CREATE_NEW);
    return keyFolder.toString();
  }

  private static String uriToGossipId(URI uri) {
    return uri.getHost() + "-" + uri.getPort();
  }

  @Override
  public void start() {
    this.gossipManager.init();
  }

  @Override
  public void shutdown() {
    this.gossipManager.shutdown();
  }

  @Override
  public List<RambleMember> getMembers() {
    return this.gossipManager
            .getLiveMembers()
            .stream()
            .map(mem -> new RambleMember(mem.getUri(),
                    Integer.parseInt(mem.getProperties().get(MESSAGE_SYNC_PORT_KEY))))
            .collect(Collectors.toList());
  }
}
