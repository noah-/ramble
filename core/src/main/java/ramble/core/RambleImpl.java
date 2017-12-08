package ramble.core;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.protobuf.ByteString;
import org.apache.log4j.Logger;
import ramble.api.IdGenerator;
import ramble.api.MembershipService;
import ramble.api.Ramble;
import ramble.api.RambleMember;
import ramble.api.RambleMessage;
import ramble.crypto.MessageSigner;
import ramble.db.DbStoreFactory;
import ramble.db.api.DbStore;
import ramble.membership.MembershipServiceFactory;
import ramble.messagesync.MessageBroadcaster;
import ramble.messagesync.MessageSyncServerFactory;
import ramble.messagesync.SyncAllMessagesService;

import java.io.IOException;
import java.net.URI;
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;


/**
 * Main implementation of {@link Ramble}.
 */
public class RambleImpl implements Ramble {

  private static final Logger LOG = Logger.getLogger(RambleImpl.class);

  private final MembershipService membershipService;
  private final ServiceManager serviceManager;
  private final PublicKey publicKey;
  private final PrivateKey privateKey;
  private final String id;
  private final DbStore dbStore;
  private final BlockingQueue<RambleMessage.Message> messageQueue;
  private final MessageBroadcaster messageBroadcaster;

  public RambleImpl(List<URI> peers, PublicKey publicKey, PrivateKey privateKey, int gossipPort, int messageSyncPort)
          throws IOException {

    Set<Service> services = new HashSet<>();

    this.privateKey = privateKey;
    this.publicKey = publicKey;
    this.id = IdGenerator.createId(gossipPort, messageSyncPort);
    this.dbStore = DbStoreFactory.getDbStore(this.id);
    this.membershipService = MembershipServiceFactory.buildMembershipService(
            peers,
            publicKey,
            privateKey,
            gossipPort,
            messageSyncPort,
            this.id);
    this.messageQueue = new ArrayBlockingQueue<>(1024);
    this.messageBroadcaster = new MessageBroadcaster(this.id, this.membershipService);

    services.add(this.membershipService);
    services.add(MessageSyncServerFactory.getMessageSyncServer(this.dbStore, messageSyncPort));
    services.add(new SyncAllMessagesService(this.membershipService, this.id, this.messageQueue));
    services.add(this.messageBroadcaster);
    this.serviceManager = new ServiceManager(services);
  }

  @Override
  public void start() {
    this.serviceManager.startAsync();
  }

  @Override
  public void post(String message) {
    LOG.info("Posting message: " + message);

    // Create signed message
    RambleMessage.SignedMessage signedMessage = buildSignedMessage(message);

    // Store the message in the local db
    DbStoreFactory.getDbStore(this.id).store(signedMessage);

    // Broadcast the message
    try {
      this.messageBroadcaster.broadcastMessage(signedMessage);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    try {
      this.messageQueue.put(signedMessage.getMessage());
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public BlockingQueue<RambleMessage.Message> listen() {
    return this.messageQueue;
  }

  @Override
  public void shutdown() {
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
  public Set<RambleMember> getMembers() {
    return this.membershipService.getMembers();
  }

  private RambleMessage.SignedMessage buildSignedMessage(String message) {
    byte[] digest = generateMessageDigest(message);

    RambleMessage.Message rambleMessage = RambleMessage.Message.newBuilder()
            .setMessage(message)
            .setTimestamp(System.currentTimeMillis())
            .setSourceId(this.id)
            .setMessageDigest(ByteString.copyFrom(digest))
            .build();

    try {
      return RambleMessage.SignedMessage.newBuilder()
              .setMessage(rambleMessage)
              .setSignature(ByteString.copyFrom(MessageSigner.sign(this.privateKey, rambleMessage.toByteArray())))
              .setPublicKey(ByteString.copyFrom(this.publicKey.getEncoded()))
              .build();
    } catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private byte[] generateMessageDigest(String message) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(message.getBytes(StandardCharsets.UTF_8));
      return md.digest();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
