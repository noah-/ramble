package ramble.gossip.apache;

import org.apache.gossip.GossipSettings;
import org.apache.gossip.Member;
import org.apache.gossip.RemoteMember;
import org.apache.gossip.manager.GossipManager;
import org.apache.gossip.manager.GossipManagerBuilder;
import org.apache.gossip.manager.handlers.MessageHandlerFactory;
import org.apache.gossip.manager.handlers.TypedMessageHandler;
import org.apache.gossip.model.RambleBulkMessage;
import org.apache.log4j.Logger;
import ramble.crypto.URIUtils;
import ramble.gossip.api.GossipMember;
import ramble.gossip.api.GossipPeer;
import ramble.gossip.api.GossipService;

import java.net.URI;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class ApacheGossipService implements GossipService {

  private static final String GOSSIP_CLUSTER_NAME = "ramble";
  private static final String MESSAGE_SYNC_PORT_KEY = "ramble.message-sync.port";
  private static final Logger LOG = Logger.getLogger(ApacheGossipService.class);

  private final GossipManager gossipManager;
  private final PublicKey publicKey;
  private final PrivateKey privateKey;

  @SuppressWarnings("unchecked")
  public ApacheGossipService(List<GossipPeer> peers, PublicKey publicKey, PrivateKey privateKey, int gossipPort,
                             int messageSyncPort, String id) {

    List<Member> gossipMembers = peers.stream()
            .map(peer -> new RemoteMember(GOSSIP_CLUSTER_NAME, peer.getPeerURI(), URIUtils.uriToId(peer.getPeerURI())))
            .collect(Collectors.toList());

    this.publicKey = publicKey;
    this.privateKey = privateKey;

    GossipSettings gossipSettings = new GossipSettings();
    gossipSettings.setDistribution("exponential");
    gossipSettings.setProtocolManagerClass(RambleProtocolManager.class.getName());
    gossipSettings.setActiveGossipClass(RambleGossiper.class.getName());

    Map<String, String> properties = new HashMap<>();
    properties.put(MESSAGE_SYNC_PORT_KEY, Integer.toString(messageSyncPort));

    this.gossipManager = GossipManagerBuilder.newBuilder()
            .cluster(GOSSIP_CLUSTER_NAME)
            .uri(URI.create("udp://127.0.0.1:" + gossipPort))
            .properties(properties)
            // There seems to be a bug in Apache Gossip where it only accepts ids of a certain form
            .id("127.0.0.1-" + gossipPort)
            .gossipMembers(gossipMembers)
            .gossipSettings(gossipSettings)
            .listener(((member, gossipState) -> LOG.info("Member " + member + " reported status " + gossipState)))
            .messageHandler(MessageHandlerFactory.concurrentHandler(MessageHandlerFactory.defaultHandler(),
                    new TypedMessageHandler(RambleBulkMessage.class, new RambleMessageHandler(id))))
            .build();
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
  public List<GossipMember> getMembers() {
    return this.gossipManager
            .getLiveMembers()
            .stream()
            .map(mem -> new GossipMember(mem.getUri(),
                    Integer.parseInt(mem.getProperties().get(MESSAGE_SYNC_PORT_KEY))))
            .collect(Collectors.toList());
  }
}
