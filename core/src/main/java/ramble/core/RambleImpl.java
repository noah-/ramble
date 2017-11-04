package ramble.core;

import org.apache.commons.cli.ParseException;

import org.apache.log4j.Logger;

import ramble.api.Ramble;
import ramble.gossip.core.GossipServiceFactory;
import ramble.gossip.api.GossipPeer;
import ramble.gossip.api.GossipService;
import ramble.gossip.api.IncomingMessage;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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

  public RambleImpl(URI gossipURI, List<String> peers)
          throws InterruptedException, IOException, URISyntaxException, ParseException {
    this(GossipServiceFactory.buildGossipService(gossipURI, peers.stream()
            .map(p -> new GossipPeer(URI.create(p))).collect(Collectors.toList())));
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
  public BlockingQueue<IncomingMessage> listen() {
    return this.gossipService.subscribe();
  }

  @Override
  public void shutdown() {
    this.gossipService.shutdown();
  }
}
