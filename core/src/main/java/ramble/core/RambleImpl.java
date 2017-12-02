package ramble.core;

import org.apache.log4j.Logger;
import ramble.api.Ramble;
import ramble.api.RambleMessage;
import ramble.crypto.URIUtils;
import ramble.db.DbStoreFactory;
import ramble.gossip.api.GossipPeer;
import ramble.gossip.api.GossipService;
import ramble.gossip.core.GossipServiceFactory;
import ramble.messagesync.MessageSyncClientFactory;
import ramble.messagesync.MessageSyncServerFactory;
import ramble.messagesync.api.MessageSyncClient;
import ramble.messagesync.api.MessageSyncServer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * Main implementation of {@link Ramble}. Currently just runs a {@link GossipService}. More RAMBLE-specific logic can
 * be added here.
 */
public class RambleImpl implements Ramble {

  private static final Logger LOG = Logger.getLogger(RambleImpl.class);

  private final GossipService gossipService;
  private final MessageSyncServer messageSyncServer;

  public RambleImpl(URI gossipURI, List<URI> peers, PublicKey publicKey, PrivateKey privateKey, int messageSyncPort)
          throws InterruptedException, IOException, URISyntaxException {

    this.gossipService = GossipServiceFactory.buildGossipService(
            gossipURI,
            peers.stream().map(GossipPeer::new).collect(Collectors.toList()),
            publicKey,
            privateKey);

    this.messageSyncServer = MessageSyncServerFactory.getMessageSyncServer(
            DbStoreFactory.getDbStore(URIUtils.uriToId(gossipURI)), messageSyncPort);

    ScheduledFuture syncProtocolFuture = Executors.newScheduledThreadPool(1)
            .scheduleAtFixedRate(this::runSyncProtocol, 5, 15, TimeUnit.SECONDS);

    LOG.info("Running Gossip service on " + this.gossipService.getURI());
  }

  private void runSyncProtocol() {
    try {
      System.out.println("Running sync protocol");
      List<URI> peers = this.gossipService.getConnectedPeers();
      System.out.println("Connected peers: " + Arrays.toString(peers.toArray()));

      if (peers.size() > 0) {
        Random rand = new Random();
        URI targetURI = peers.get(rand.nextInt(peers.size()));

        // TODO fix so port # is correct
        MessageSyncClient client = MessageSyncClientFactory.getMessageSyncClient(targetURI.getHost(),
                targetURI.getPort() + 1000);
        client.connect();

        Set<RambleMessage.SignedMessage> messages = client.syncMessages();

//        System.out.println("Got messages: " + Arrays.toString(messages.toArray()));
      }
    } catch (Throwable t) {
      LOG.error("Sync protocol failed", t);
    }
  }

  @Override
  public void start() {
    this.gossipService.start();
    try {
      this.messageSyncServer.start();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void post(String message) {
    this.gossipService.gossip(message);
  }

  @Override
  public void shutdown() {
    this.gossipService.shutdown();
    this.messageSyncServer.stop();
  }
}
