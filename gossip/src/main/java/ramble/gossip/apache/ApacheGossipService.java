package ramble.gossip.apache;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import org.apache.gossip.GossipService;
import org.apache.gossip.LocalGossipMember;
import org.apache.gossip.StartupSettings;
import org.apache.gossip.crdt.GrowOnlySet;
import org.apache.gossip.model.SharedGossipDataMessage;


public class ApacheGossipService implements ramble.gossip.api.GossipService {

  private static final String MESSAGES_SET_KEY = "messages";

  private GossipService gossipService;

  public ApacheGossipService() throws IOException, URISyntaxException, InterruptedException {
    this.gossipService = new GossipService(
            StartupSettings.fromJSONFile(new File("gossip/src/main/resources/gossip-settings.json")));
  }

  @Override
  public void gossip(String message) {
    SharedGossipDataMessage gossipDataMessage = new SharedGossipDataMessage();
    gossipDataMessage.setKey(MESSAGES_SET_KEY);
    gossipDataMessage.setPayload(new GrowOnlySet<>(Sets.newHashSet(message)));
    gossipDataMessage.setTimestamp(System.currentTimeMillis());
    gossipDataMessage.setExpireAt(Long.MAX_VALUE);
    this.gossipService.getGossipManager().merge(gossipDataMessage);
  }

  @Override
  public void start() {
    this.gossipService.start();
  }

  @Override
  public void shutdown() {
    this.gossipService.shutdown();
  }

  @Override
  public void printMessages() {
    System.out.println("Messages: " + this.gossipService.getGossipManager().findCrdt(MESSAGES_SET_KEY));
  }

  @Override
  public List<String> getPeers() {
    return this.gossipService.getGossipManager().getLiveMembers().stream().map(LocalGossipMember::toString).collect(
            Collectors.toList());
  }
}
