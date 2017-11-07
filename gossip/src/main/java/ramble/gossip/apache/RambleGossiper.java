package ramble.gossip.apache;

import com.codahale.metrics.MetricRegistry;

import org.apache.gossip.LocalMember;
import org.apache.gossip.manager.AbstractActiveGossiper;
import org.apache.gossip.manager.GossipCore;
import org.apache.gossip.manager.GossipManager;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class RambleGossiper extends AbstractActiveGossiper {

  // TODO this class should basically have a gossip(RamblePayload) method
  // That just selects any random node and gossips to it

  private ScheduledExecutorService scheduledExecutorService;
  private final BlockingQueue<Runnable> workQueue;
  private ThreadPoolExecutor threadService;

  public RambleGossiper(GossipManager gossipManager, GossipCore gossipCore, MetricRegistry registry) {
    super(gossipManager, gossipCore, registry);
    scheduledExecutorService = Executors.newScheduledThreadPool(2);
    workQueue = new ArrayBlockingQueue<>(1024);
    threadService = new ThreadPoolExecutor(1, 30, 1, TimeUnit.SECONDS, workQueue,
            new ThreadPoolExecutor.DiscardOldestPolicy());
  }

  @Override
  public void init() {
    super.init();
    scheduledExecutorService.scheduleAtFixedRate(() -> threadService.execute(this::sendToALiveMember), 0,
            gossipManager.getSettings().getGossipInterval(), TimeUnit.MILLISECONDS);

    scheduledExecutorService.scheduleAtFixedRate(this::sendToDeadMember, 0,
            gossipManager.getSettings().getGossipInterval(), TimeUnit.MILLISECONDS);

    scheduledExecutorService.scheduleAtFixedRate(
            () -> sendPerNodeData(gossipManager.getMyself(),
                    selectPartner(gossipManager.getLiveMembers())),
            0, gossipManager.getSettings().getGossipInterval(), TimeUnit.MILLISECONDS);

    scheduledExecutorService.scheduleAtFixedRate(
            () -> sendSharedData(gossipManager.getMyself(),
                    selectPartner(gossipManager.getLiveMembers())),
            0, gossipManager.getSettings().getGossipInterval(), TimeUnit.MILLISECONDS);
  }

  @Override
  public void shutdown() {
    super.shutdown();
    scheduledExecutorService.shutdown();
    try {
      scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      LOGGER.debug("Issue during shutdown", e);
    }
    sendShutdownMessage();
    threadService.shutdown();
    try {
      threadService.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      LOGGER.debug("Issue during shutdown", e);
    }
  }

  private void sendToALiveMember() {
    LocalMember member = selectPartner(gossipManager.getLiveMembers());
    sendMembershipList(gossipManager.getMyself(), member);
  }

  private void sendToDeadMember() {
    LocalMember member = selectPartner(gossipManager.getDeadMembers());
    sendMembershipList(gossipManager.getMyself(), member);
  }

  private void sendShutdownMessage() {
    List<LocalMember> l = gossipManager.getLiveMembers();
    int sendTo = l.size() < 3 ? 1 : l.size() / 2;
    for (int i = 0; i < sendTo; i++) {
      threadService.execute(() -> sendShutdownMessage(gossipManager.getMyself(), selectPartner(l)));
    }
  }

  @Override
  public LocalMember selectPartner(List<LocalMember> memberList) {
    return null;
  }
}
