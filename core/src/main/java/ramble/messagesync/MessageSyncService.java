package ramble.messagesync;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.Service;
import org.apache.log4j.Logger;
import ramble.api.RambleMessage;
import ramble.core.RambleImpl;
import ramble.crypto.MessageSigner;
import ramble.db.api.DbStore;
import ramble.api.RambleMember;
import ramble.api.MembershipService;
import ramble.messagesync.api.MessageSyncClient;
import ramble.messagesync.api.MessageSyncServer;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MessageSyncService extends AbstractScheduledService implements Service {

  private static final Logger LOG = Logger.getLogger(RambleImpl.class);

  private final MembershipService gossipService;
  private final DbStore dbStore;
  private final MessageSyncServer messageSyncServer;
  private final String id;
  private final Random rand;

  public MessageSyncService(MembershipService gossipService, DbStore dbStore, int port, String id) {
    this.gossipService = gossipService;
    this.dbStore = dbStore;
    this.messageSyncServer = MessageSyncServerFactory.getMessageSyncServer(dbStore, port);
    this.id = id;
    this.rand = new Random();
  }

  @Override
  public void startUp() throws InterruptedException {
    this.messageSyncServer.start();
  }

  @Override
  public void shutDown() {
    this.messageSyncServer.stop();
  }

  @Override
  protected void runOneIteration() throws InterruptedException {
    LOG.info("[id = " + this.id + "] Running message sync protocol");

    List<RambleMember> peers = this.gossipService.getMembers();
    if (!peers.isEmpty()) {
      RambleMember target = getTargetURI(peers);

      MessageSyncClient client = MessageSyncClientFactory.getMessageSyncClient(target.getUri().getHost(),
              target.getMessageSyncPort());

      try {
        client.connect();

        Set<RambleMessage.SignedMessage> messages = client.syncMessages();
        for (RambleMessage.SignedMessage signedMessage : messages) {
          if (MessageSigner.verify(signedMessage.getPublicKey().toByteArray(),
                  signedMessage.getMessage().toByteArray(), signedMessage.getSignature().toByteArray())) {

            if (!this.dbStore.exists(signedMessage)) {
              LOG.info("[id = " + this.id + "] Message-sync got new message from " +
                      signedMessage.getMessage().getSourceId() + " - " + signedMessage.getMessage().getMessage());
              this.dbStore.store(signedMessage);
            }
          }
        }
      } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | InvalidKeySpecException e) {
        throw new RuntimeException(e);
      } finally {
        client.disconnect();
      }
    }
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedRateSchedule(1500, 1000, TimeUnit.MILLISECONDS);
  }

  private RambleMember getTargetURI(List<RambleMember> peers) {
    return peers.get(this.rand.nextInt(peers.size()));
  }
}
