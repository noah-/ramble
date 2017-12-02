package ramble.gossip.apache;

import com.google.protobuf.ByteString;
import org.apache.gossip.GossipSettings;
import org.apache.gossip.LocalMember;
import org.apache.gossip.Member;
import org.apache.gossip.RemoteMember;
import org.apache.gossip.manager.GossipManager;
import org.apache.gossip.manager.GossipManagerBuilder;
import org.apache.gossip.manager.handlers.MessageHandlerFactory;
import org.apache.gossip.manager.handlers.TypedMessageHandler;
import org.apache.gossip.model.RambleBulkMessage;
import org.apache.log4j.Logger;
import ramble.api.RambleMessage;
import ramble.crypto.MessageSigner;
import ramble.crypto.URIUtils;
import ramble.gossip.api.GossipPeer;
import ramble.gossip.api.GossipService;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.List;
import java.util.stream.Collectors;


public class ApacheGossipService implements GossipService {

  private static final String GOSSIP_CLUSTER_NAME = "ramble";
  private static final Logger LOG = Logger.getLogger(ApacheGossipService.class);

  private final GossipManager gossipManager;
  private final URI gossipURI;
  private final PublicKey publicKey;
  private final PrivateKey privateKey;

  @SuppressWarnings("unchecked")
  public ApacheGossipService(URI uri, List<GossipPeer> peers, PublicKey publicKey, PrivateKey privateKey) {

    List<Member> gossipMembers = peers.stream()
            .map(peer -> new RemoteMember(GOSSIP_CLUSTER_NAME, peer.getPeerURI(),
                    peer.getPeerURI().getHost() + "-" + peer.getPeerURI().getPort()))
            .collect(Collectors.toList());

    this.gossipURI = uri;
    this.publicKey = publicKey;
    this.privateKey = privateKey;

    GossipSettings gossipSettings = new GossipSettings();
    gossipSettings.setDistribution("exponential");
    gossipSettings.setProtocolManagerClass(RambleProtocolManager.class.getName());
    gossipSettings.setActiveGossipClass(RambleGossiper.class.getName());
    gossipSettings.setGossipInterval(1000); // TODO: set for testing purposes

    this.gossipManager = GossipManagerBuilder.newBuilder()
            .cluster(GOSSIP_CLUSTER_NAME)
            .uri(uri)
            .id(URIUtils.uriToId(uri))
            .gossipMembers(gossipMembers)
            .gossipSettings(gossipSettings)
            .listener(((member, gossipState) -> LOG.info("Member " + member + " reported status " + gossipState)))
            .messageHandler(MessageHandlerFactory.concurrentHandler(MessageHandlerFactory.defaultHandler(),
                    new TypedMessageHandler(RambleBulkMessage.class, new RambleMessageHandler(URIUtils.uriToId(uri)))))
            .build();
  }

  @Override
  public void gossip(String message) {
    LOG.info("Sending message: " + message);

    byte[] digest;
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(message.getBytes(StandardCharsets.UTF_8));
      digest = md.digest();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

    RambleMessage.Message rambleMessage = RambleMessage.Message.newBuilder()
            .setMessage(message)
            .setTimestamp(System.currentTimeMillis())
            .setSourceId(this.gossipManager.getMyself().getId())
            .setMessageDigest(ByteString.copyFrom(digest))
            .build();

    RambleMessage.SignedMessage signedMessage;
    try {
      signedMessage = RambleMessage.SignedMessage.newBuilder()
              .setMessage(rambleMessage)
              .setSignature(ByteString.copyFrom(MessageSigner.sign(this.privateKey, rambleMessage.toByteArray())))
              .setPublicKey(ByteString.copyFrom(this.publicKey.getEncoded()))
              .build();
    } catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

    this.gossipManager.gossipRambleMessage(signedMessage);
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
  public List<URI> getConnectedPeers() {
    return this.gossipManager.getLiveMembers().stream().map(LocalMember::getUri).collect(Collectors.toList());
  }

  @Override
  public String getURI() {
    return this.gossipURI.toString();
  }
}
