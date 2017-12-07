package ramble.p2p;

import ramble.db.h2.H2DbStore;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class AntiEntropy {

    private static final long BLOCK_TIME_PERIOD = 300000; // 5 mins
    private static AtomicLong _lastVerifiedTS = new AtomicLong(0);

    private final H2DbStore dbStore;
    private long currentTS;
    private HashMap<Long,HashSet<String>> blockCache = new HashMap<>();
    private MessageService ms;

    public AntiEntropy(MessageService ms, String db, long ts, long nts) {
        if (_lastVerifiedTS.get() < ts)
            _lastVerifiedTS.set(ts);

        this.ms = ms;
        currentTS = (nts / BLOCK_TIME_PERIOD) * BLOCK_TIME_PERIOD;;
        dbStore = H2DbStore.getOrCreateStore(db);
    }

    public void computeComplement(HashSet<String> a, HashSet<String> b) {
        for (String s : b) {
            if (a.remove(s)) {
                b.remove(s);
            }
        }
    }

    public HashSet<String> getDigestBlock(long ts) {
        return dbStore.getRange(ts - BLOCK_TIME_PERIOD, ts).stream().map(
                msg -> msg.getMessage().getMessage()).collect(
                Collectors.toCollection(HashSet::new));
    }

    public void run() {
        long current = currentTS;

        while (current > _lastVerifiedTS.get()) {
            HashSet<String> a = getDigestBlock(current);

            HashSet<String> copy = new HashSet<String>();
            for (String s : a) {
                copy.add(s);
                System.out.println(s);
            }
            a = copy;

            ms.sendBlock(a);
            HashSet<String> b = null;
            try {
                b = ms.getBlock();
            } catch (InterruptedException e)
            {}

            computeComplement(a, b);

            if (blockCache.containsKey(current)) {
                blockCache.get(current).addAll(b);
            } else {
                blockCache.put(current, b);
            }

            current -= BLOCK_TIME_PERIOD;
        }
    }

    public void flushCache(){
        System.out.println("Verified TS Before: " + _lastVerifiedTS.get());

        for (long k : blockCache.keySet()) {
            HashSet<String> hs = blockCache.get(k);
            // TODO
            // request messages with the following digests in hs
            // save them to database
            for (String s : hs)
                System.out.println(s);
        }

        _lastVerifiedTS.set(currentTS);
        System.out.println("Verified TS After: " + _lastVerifiedTS.get());
        blockCache.clear();
    }

}
