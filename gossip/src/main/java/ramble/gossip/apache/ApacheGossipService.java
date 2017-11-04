package ramble.gossip.apache;

import org.apache.gossip.GossipSettings;
import org.apache.gossip.LocalMember;
import org.apache.gossip.Member;
import org.apache.gossip.RemoteMember;
import org.apache.gossip.manager.GossipManager;
import org.apache.gossip.manager.GossipManagerBuilder;
import org.apache.gossip.model.PerNodeDataMessage;

import org.apache.log4j.Logger;

import ramble.gossip.api.GossipPeer;
import ramble.gossip.api.GossipService;
import ramble.gossip.api.IncomingMessage;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;


public class ApacheGossipService implements GossipService {

  private static final String GOSSIP_CLUSTER_NAME = "ramble";
  private static final Logger LOG = Logger.getLogger(ApacheGossipService.class);

  private final GossipManager gossipManager;
  private final URI gossipURI;
  private final BlockingQueue<IncomingMessage> messages;

  public ApacheGossipService(URI uri, List<GossipPeer> peers)
          throws IOException, URISyntaxException, InterruptedException {

    List<Member> gossipMembers = peers.stream()
            .map(peer -> new RemoteMember(GOSSIP_CLUSTER_NAME, peer.getPeerURI(), peer.getPeerURI().toString()))
            .collect(Collectors.toList());

    this.gossipURI = uri;

    this.gossipManager = GossipManagerBuilder.newBuilder()
            .cluster(GOSSIP_CLUSTER_NAME)
            .uri(uri)
            .id(uri.toString())
            .gossipMembers(gossipMembers)
            .gossipSettings(new GossipSettings())
            .build();

    this.messages = new LinkedBlockingQueue<>();
    this.gossipManager.registerPerNodeDataSubscriber((nodeId, key, oldValue, newValue) -> {
      try {
        LOG.info("Got message " + newValue);
        messages.put(new IncomingMessage(nodeId, newValue.toString()));
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });
  }

  @Override
  public void gossip(String message) {
    LOG.info("Sending message: " + message);

    PerNodeDataMessage gossipDataMessage = new PerNodeDataMessage();
    gossipDataMessage.setKey(UUID.randomUUID().toString());
    gossipDataMessage.setPayload(message);
    gossipDataMessage.setTimestamp(System.currentTimeMillis());
    this.gossipManager.gossipPerNodeData(gossipDataMessage);
  }

  @Override
  public void start() {
    this.gossipManager.init();

    Thread printMembersThread = new Thread(() -> {
      while (true) {
//        LOG.info("Live Peers: " + gossipManager.getLiveMembers());
//        LOG.info("Dead Peers: " + gossipManager.getDeadMembers());
        try {
          Thread.sleep(10000);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
    printMembersThread.start();
  }

  @Override
  public void shutdown() {
    this.gossipManager.shutdown();
  }

  @Override
  public BlockingQueue<IncomingMessage> subscribe() {
    return messages;
  }

  @Override
  public List<String> getConnectedPeers() {
    return this.gossipManager.getLiveMembers().stream().map(LocalMember::toString).collect(Collectors.toList());
  }

  @Override
  public String getURI() {
    return this.gossipURI.toString();
  }
}
