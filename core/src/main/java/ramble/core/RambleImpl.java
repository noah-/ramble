package ramble.core;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import org.apache.log4j.Logger;
import ramble.api.IdGenerator;
import ramble.api.MembershipService;
import ramble.api.Ramble;
import ramble.api.RambleMember;
import ramble.api.RambleMessage;
import ramble.db.DbStoreFactory;
import ramble.db.api.DbStore;
import ramble.membership.MembershipServiceFactory;
import ramble.messagesync.BootstrapProtocol;
import ramble.messagesync.ComputeComplementService;
import ramble.messagesync.DefaultMessageSyncServerHandler;
import ramble.messagesync.MessageBroadcaster;
import ramble.messagesync.MessageSyncServerFactory;

import java.io.IOException;
import java.net.URI;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
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

  private static final int MESSAGE_BROADCAST_FANOUT = 8;

  private final List<URI> peers;
  private final MembershipService membershipService;
  private final PublicKey publicKey;
  private final PrivateKey privateKey;
  private final String id;
  private final DbStore dbStore;
  private final BlockingQueue<RambleMessage.Message> messageQueue;
  private final MessageBroadcaster messageBroadcaster;
  private final ServiceManager serviceManager;
  private final URI bootstrapTarget;

  public RambleImpl(URI bootstrapTarget, List<URI> peers, PublicKey publicKey, PrivateKey privateKey, int gossipPort,
                    int messageSyncPort) throws IOException {

    this.bootstrapTarget = bootstrapTarget;
    this.peers = peers;
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
    this.messageBroadcaster = new MessageBroadcaster(this.id, this.membershipService, MESSAGE_BROADCAST_FANOUT);

    List<Service> services = new ArrayList<>();
    services.add(this.messageBroadcaster);
    services.add(MessageSyncServerFactory.getMessageSyncServer(this.id,
            new DefaultMessageSyncServerHandler(this, this.dbStore), messageSyncPort));
    services.add(new ComputeComplementService(this.membershipService, this.dbStore, this.messageQueue, this.id));

    this.serviceManager = new ServiceManager(services);
  }

  @Override
  public void start() throws InterruptedException {
    LOG.info("[id = " + this.id + "] Starting Ramble service");

    // Start all services before joining the membership service so that external nodes don't fail to connect to any
    // local services before they have started
    this.serviceManager.startAsync().awaitHealthy();

    // Run the boostrap protocol before joining the cluster
    if (this.bootstrapTarget != null) runBootstrap();

    // Once all services are up and running, start the membership service so that other nodes can start communicating
    // with all local services
    this.membershipService.startAsync().awaitRunning();
  }

  @Override
  public void post(String message) throws InterruptedException {
    LOG.info("[id = " + this.id +  "] Posting message: " + message);

    // Create signed message
    RambleMessage.SignedMessage signedMessage = MessageBuilder.buildSignedMessage(this.id, message, this.publicKey,
            this.privateKey);

    // Store the message in the local db
    DbStoreFactory.getDbStore(this.id).store(signedMessage);

    // Broadcast the message
    broadcast(signedMessage);
  }

  @Override
  public BlockingQueue<RambleMessage.Message> listen() {
    return this.messageQueue;
  }

  @Override
  public void shutdown() {
    LOG.info("[id = " + this.id + "] Shutting down Ramble service");

    // Leave the membership cluster
    this.membershipService.stopAsync();//.awaitTerminated();

    // Once the membership cluster has stopped, then shutdown the rest of the services
    this.serviceManager.stopAsync();//.awaitStopped();
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

  @Override
  public void broadcast(RambleMessage.SignedMessage message) throws InterruptedException {
    this.messageBroadcaster.broadcastMessage(message);
    this.messageQueue.put(message.getMessage());
  }

  @Override
  public PublicKey getPublicKey() {
    return this.publicKey;
  }

  @Override
  public PrivateKey getPrivateKey() {
    return this.privateKey;
  }

  private void runBootstrap() throws InterruptedException {
    new BootstrapProtocol(this.bootstrapTarget, this.id, this.dbStore, this.messageQueue).run();
  }
}
