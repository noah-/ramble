package ramble.core;

import com.google.common.base.Joiner;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.protobuf.ByteString;
import org.apache.log4j.Logger;
import ramble.api.Ramble;
import ramble.api.RambleMessage;
import ramble.crypto.MessageSigner;
import ramble.db.DbStoreFactory;
import ramble.db.api.DbStore;
import ramble.api.RambleMember;
import ramble.gossip.api.GossipPeer;
import ramble.api.MembershipService;
import ramble.messagesync.MessageSyncService;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Main implementation of {@link Ramble}. Currently just runs a {@link MembershipService}. More RAMBLE-specific logic can
 * be added here.
 */
public class RambleImpl implements Ramble {

  private static final Logger LOG = Logger.getLogger(RambleImpl.class);

  private final MembershipService membershipService;
  private final ServiceManager serviceManager;
  private final PublicKey publicKey;
  private final PrivateKey privateKey;
  private final String id;
  private final DbStore dbStore;

  public RambleImpl(List<URI> peers, PublicKey publicKey, PrivateKey privateKey, int gossipPort, int messageSyncPort)
          throws IOException {

    Set<Service> services = new HashSet<>();

    this.privateKey = privateKey;
    this.publicKey = publicKey;
    this.id = createId(gossipPort, messageSyncPort);
    this.dbStore = DbStoreFactory.getDbStore(this.id);
    this.membershipService = MembershipServiceFactory.buildMembershipService(
            peers.stream().map(GossipPeer::new).collect(Collectors.toList()),
            publicKey,
            privateKey,
            gossipPort,
            messageSyncPort,
            this.id);

    MessageSyncService messageSyncService = new MessageSyncService(
            this.membershipService,
            this.dbStore,
            messageSyncPort,
            this.id);

    services.add(messageSyncService);

    this.serviceManager = new ServiceManager(services);
  }

  @Override
  public void start() {
    this.membershipService.start();
    this.serviceManager.startAsync();
  }

  @Override
  public void post(String message) {
    LOG.info("Posting message: " + message);

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
            .setSourceId(this.id)
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

    DbStoreFactory.getDbStore(this.id).store(signedMessage);
  }

  @Override
  public void shutdown() {
    this.membershipService.shutdown();
    this.serviceManager.stopAsync();
  }

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public Set<RambleMessage.Message> getAllMessages() {
    return this.dbStore
            .getAllMessages()
            .stream()
            .map(RambleMessage.SignedMessage::getMessage)
            .collect(Collectors.toSet());
  }

  @Override
  public Set<URI> getMembers() {
    return this.membershipService
            .getMembers()
            .stream()
            .map(RambleMember::getUri)
            .collect(Collectors.toSet());
  }

  private static String createId(int gossipPort, int messageSyncPort) throws UnknownHostException {
    return Joiner.on("-").join(
            InetAddress.getLocalHost().getHostAddress(),
            gossipPort,
            messageSyncPort);
  }
}
