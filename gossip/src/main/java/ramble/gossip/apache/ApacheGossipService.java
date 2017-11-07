package ramble.gossip.apache;

import com.google.common.collect.Sets;
import org.apache.gossip.GossipSettings;
import org.apache.gossip.LocalMember;
import org.apache.gossip.Member;
import org.apache.gossip.RemoteMember;
import org.apache.gossip.crdt.GrowOnlySet;
import org.apache.gossip.manager.GossipManager;
import org.apache.gossip.manager.GossipManagerBuilder;
import org.apache.gossip.model.SharedDataMessage;
import org.apache.log4j.Logger;
import ramble.gossip.api.GossipPeer;
import ramble.gossip.api.GossipService;
import ramble.gossip.api.IncomingMessage;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;


public class ApacheGossipService implements GossipService {

  private static final String GOSSIP_CLUSTER_NAME = "ramble";
  private static final String CRDT_MESSAGE_KEY = "messageQueue";
  private static final Logger LOG = Logger.getLogger(ApacheGossipService.class);

  private final GossipManager gossipManager;
  private final URI gossipURI;
  private final BlockingQueue<IncomingMessage> messageQueue;

  @SuppressWarnings("unchecked")
  public ApacheGossipService(URI uri, List<GossipPeer> peers)
          throws IOException, URISyntaxException, InterruptedException {

    List<Member> gossipMembers = peers.stream()
            .map(peer -> new RemoteMember(GOSSIP_CLUSTER_NAME, peer.getPeerURI(),
                    peer.getPeerURI().getHost() + "-" + peer.getPeerURI().getPort()))
            .collect(Collectors.toList());

    this.gossipURI = uri;

    GossipSettings gossipSettings = new GossipSettings();
    gossipSettings.setDistribution("exponential");

    this.gossipManager = GossipManagerBuilder.newBuilder()
            .cluster(GOSSIP_CLUSTER_NAME)
            .uri(uri)
            .id(uri.getHost() + "-" + uri.getPort())
            .gossipMembers(gossipMembers)
            .gossipSettings(gossipSettings)
            .listener(((member, gossipState) -> LOG.info("Member " + member + " reported status " + gossipState)))
            .build();

    this.messageQueue = new LinkedBlockingQueue<>();
    this.gossipManager.registerSharedDataSubscriber((key, oldValue, newValue) -> {
      try {
        if (key.equals(CRDT_MESSAGE_KEY)) {
          if (oldValue == null) {
            oldValue = new GrowOnlySet<>(new HashSet<String>());
          }
          for (String message : Sets.difference(((GrowOnlySet<String>) newValue).value(),
                  ((GrowOnlySet<String>) oldValue).value())) {
            LOG.info("Adding message " + message + " to the queue");
            messageQueue.put(new IncomingMessage(message));
          }
        }
      } catch (InterruptedException e) {
        LOG.error("Interrupted while adding to message queue", e);
      }
    });
  }

  @Override
  public void gossip(String message) {
    LOG.info("Sending message: " + message);

    SharedDataMessage sharedDataMessage = new SharedDataMessage();
    sharedDataMessage.setExpireAt(Long.MAX_VALUE);
    sharedDataMessage.setKey(CRDT_MESSAGE_KEY);
    sharedDataMessage.setPayload(new GrowOnlySet<>(Sets.newHashSet(message)));
    sharedDataMessage.setTimestamp(System.currentTimeMillis());
    this.gossipManager.merge(sharedDataMessage);
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
  public BlockingQueue<IncomingMessage> subscribe() {
    return messageQueue;
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
