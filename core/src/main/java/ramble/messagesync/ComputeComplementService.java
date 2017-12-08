package ramble.messagesync;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.Service;

import ramble.api.MembershipService;
import ramble.api.MessageSyncProtocol;
import ramble.api.RambleMember;
import ramble.db.api.DbStore;
import ramble.messagesync.api.MessageSyncClient;
import ramble.messagesync.api.MessageSyncHandler;
import ramble.messagesync.api.TargetSelector;

import java.util.Set;
import java.util.concurrent.TimeUnit;


/**
 * An anti-entropy protocol iteratively syncs its messages with a target random node. This is done by dividing all local
 * messages into logical blocks that are bound by a time window. THe initiating starts at the most recent block, and
 * sends all messages digests in the block to the target node. The target node responds with a list of messages that the
 * source node is missing from the block in question. The source node then adds all these messages to its local db, and
 * then moves to the next block.
 */
public class ComputeComplementService extends AbstractScheduledService implements Service {

    private static final long BLOCK_TIME_PERIOD = 300000; // 5 minutes

    private final DbStore dbStore;
    private final TargetSelector targetSelector;
    private final MembershipService membershipService;

    public ComputeComplementService(MembershipService membershipService, DbStore dbStore, long startTimestamp, long endTimestamp) {
        this.dbStore = dbStore;
        this.targetSelector = new RandomTargetSelector();
        this.membershipService = membershipService;
    }

    @Override
    public void runOneIteration() throws Exception {
        // TODO
    }

    private void sendBlock(Set<byte[]> messageDigests, long blockStartTime, long blockEndTime) {
        RambleMember target = targetSelector.getTarget(this.membershipService.getMembers());

        MessageSyncClient messageSyncClient = MessageSyncClientFactory.getMessageSyncClient(target.getAddr(),
                target.getMessageSyncPort(), new MessageSyncHandler() {

                    @Override
                    public void handleResponse(MessageSyncClient messageSyncClient,
                                               MessageSyncProtocol.Response response) {
                        messageSyncClient.sendRequest(
                                RequestBuilder.buildGetComplementRequest(getDigestBlock(1L), 1L, 1L));
                    }
                });
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(1500, 1000, TimeUnit.MILLISECONDS);
    }

    private Set<byte[]> getDigestBlock(long ts) {
        return dbStore.getDigestRange(ts - BLOCK_TIME_PERIOD, ts);
    }
}
