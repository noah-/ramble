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
import ramble.gossip.api.GossipPeer;
import ramble.gossip.api.GossipService;
import ramble.gossip.core.GossipServiceFactory;
import ramble.messagesync.MessageSyncService;

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
 * Main implementation of {@link Ramble}. Currently just runs a {@link GossipService}. More RAMBLE-specific logic can
 * be added here.
 */
public class RambleImpl implements Ramble {

  private static final Logger LOG = Logger.getLogger(RambleImpl.class);

  private final GossipService gossipService;
  private final ServiceManager serviceManager;
  private final PublicKey publicKey;
  private final PrivateKey privateKey;
  private final String id;

  public RambleImpl(List<URI> peers, PublicKey publicKey, PrivateKey privateKey, int gossipPort, int messageSyncPort)
          throws UnknownHostException {

    Set<Service> services = new HashSet<>();

    this.privateKey = privateKey;
    this.publicKey = publicKey;
    this.id = createId(gossipPort, messageSyncPort);
    this.gossipService = GossipServiceFactory.buildGossipService(
            peers.stream().map(GossipPeer::new).collect(Collectors.toList()),
            publicKey,
            privateKey,
            gossipPort,
            messageSyncPort,
            this.id);

    MessageSyncService messageSyncService = new MessageSyncService(this.gossipService,
            DbStoreFactory.getDbStore(this.id), messageSyncPort, this.id);
    services.add(messageSyncService);

    this.serviceManager = new ServiceManager(services);
  }

  @Override
  public void start() {
    this.gossipService.start();
    this.serviceManager.startAsync();
  }

  @Override
  public void post(String message) {
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
    this.gossipService.shutdown();
    this.serviceManager.stopAsync();
  }

  private static String createId(int gossipPort, int messageSyncPort) throws UnknownHostException {
    return Joiner.on("-").join(InetAddress.getLocalHost().getHostAddress(), gossipPort, messageSyncPort);
  }

  @Override
  public String getId() {
    return this.id;
  }
}
