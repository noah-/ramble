package ramble.core;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import ramble.api.Ramble;
import ramble.api.RambleMessage;
import ramble.gossip.api.GossipPeer;
import ramble.gossip.api.GossipService;
import ramble.gossip.core.GossipServiceFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;


/**
 * Main implementation of {@link Ramble}. Currently just runs a {@link GossipService}. More RAMBLE-specific logic can
 * be added here.
 */
public class RambleImpl implements Ramble {

  private static final Logger LOG = Logger.getLogger(RambleImpl.class);

  private final GossipService gossipService;

  public RambleImpl(URI gossipURI, List<String> peers, PublicKey publicKey, PrivateKey privateKey)
          throws InterruptedException, IOException, URISyntaxException, ParseException {
    this(GossipServiceFactory.buildGossipService(
            gossipURI,
            peers.stream().map(p -> new GossipPeer(URI.create(p))).collect(Collectors.toList()),
            publicKey,
            privateKey));
  }

  private RambleImpl(GossipService gossipService)
          throws InterruptedException, IOException, URISyntaxException, ParseException {
    LOG.info("Running Gossip service on " + gossipService.getURI());

    this.gossipService = gossipService;
  }

  @Override
  public void start() {
    this.gossipService.start();
  }

  @Override
  public void post(String message) {
    this.gossipService.gossip(message);
  }

  @Override
  public BlockingQueue<RambleMessage.Message> listen() {
    return this.gossipService.subscribe();
  }

  @Override
  public void shutdown() {
    this.gossipService.shutdown();
  }
}
