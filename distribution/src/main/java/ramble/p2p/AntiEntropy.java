package ramble.p2p;

import ramble.db.persistent.PersistentDbStore;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

public class AntiEntropy {

    private static long _lastVerifiedTS;
    private long currentTS;
    private final long BLOCK_TIME_PERIOD = 300000; // 5 mins
    private final PersistentDbStore dbStore = PersistentDbStore.getOrCreateStore("anti-entropy");
    private HashMap<Long,HashSet<String>> blockCache = new HashMap<>();

    public AntiEntropy(long ts) {
        _lastVerifiedTS = ts;
    }

    public void computeComplement(HashSet<String> a, HashSet<String> b) {
        for (String s : b) {
            if (a.remove(s)) {
                b.remove(s);
            }
        }
    }

    public void runAntiEntropy(long current) {
        currentTS = current;
        while (current > _lastVerifiedTS) {
            long end = (current / BLOCK_TIME_PERIOD) * BLOCK_TIME_PERIOD;
            HashSet<String> a = dbStore.getRange(end - BLOCK_TIME_PERIOD, end).stream().map(
                    msg -> msg.getMessage().getMessage()).collect(
                    Collectors.toCollection(HashSet::new));
            // TODO
            // serialize and send to other peer
            // wait to recieve set from other peer
            HashSet<String> b = new HashSet<String>(); // placeholder for receive set
            computeComplement(a, b);

            if (blockCache.containsKey(end)) {
                blockCache.get(end).addAll(b);
            } else {
                blockCache.put(end, b);
            }
        }
    }

    public void flushCache(){
        for (long k : blockCache.keySet()) {
            HashSet<String> hs = blockCache.get(k);
            // TODO
            // request messages with the following digests in hs
            // save them to database
        }

        _lastVerifiedTS = currentTS;
        blockCache.clear();
    }

}
