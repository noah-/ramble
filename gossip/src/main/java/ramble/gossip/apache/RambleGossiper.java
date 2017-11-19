package ramble.gossip.apache;

import com.codahale.metrics.MetricRegistry;
import org.apache.gossip.LocalMember;
import org.apache.gossip.manager.GossipCore;
import org.apache.gossip.manager.GossipManager;
import org.apache.gossip.manager.SimpleActiveGossiper;
import org.apache.gossip.model.RambleBulkMessage;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class RambleGossiper extends SimpleActiveGossiper {

  // TODO this class should basically have a gossip(RamblePayload) method
  // That just selects any random node and gossips to it

  private final ScheduledExecutorService rambleMessageExecutorService;

  public RambleGossiper(GossipManager gossipManager, GossipCore gossipCore, MetricRegistry registry) {
    super(gossipManager, gossipCore, registry);
    this.rambleMessageExecutorService = Executors.newScheduledThreadPool(10);
  }

  @Override
  public void init() {
    super.init();
    this.rambleMessageExecutorService.scheduleAtFixedRate(
            () -> sendRambleMessages(selectPartner(gossipManager.getLiveMembers())), 0,
            gossipManager.getSettings().getGossipInterval(), TimeUnit.MILLISECONDS);
  }

  @Override
  public void shutdown() {
    super.shutdown();
  }

  /**
   * For now, we simply send all ramble messages
   */
  private void sendRambleMessages(LocalMember dest) {
    if (dest != null) {
      ramble.api.RambleMessage.BulkSignedMessage.Builder builder = ramble.api.RambleMessage.BulkSignedMessage.newBuilder();
      for (ramble.api.RambleMessage.SignedMessage rambleMessage : this.gossipCore.getRambleMessages()) {
        builder.addSignedMessage(rambleMessage);
      }
      RambleBulkMessage rambleBulkMessage = new RambleBulkMessage();
      rambleBulkMessage.setBulkSignedMessage(builder.build().toByteArray());
      this.gossipCore.sendOneWay(rambleBulkMessage, dest.getUri());
    }
  }
}
