package ramble.p2p;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.Service;
import ramble.db.h2.H2DbStore;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class AntiEntropy extends AbstractScheduledService implements Service {

  private static final long BLOCK_TIME_PERIOD = 300000; // 5 mins
  private static AtomicLong _lastVerifiedTS = new AtomicLong(0);

  private final H2DbStore dbStore;
  private long currentTS;
  private Map<Long, Set<byte[]>> blockCache = new HashMap<>();
  private LocalMessageService ms;

  public AntiEntropy(LocalMessageService ms, String db, long ts, long nts) {
    if (_lastVerifiedTS.get() < ts)
      _lastVerifiedTS.set(ts);

    System.out.println("Verified TS Before: " + _lastVerifiedTS.get());
    this.ms = ms;
    currentTS = (nts / BLOCK_TIME_PERIOD) * BLOCK_TIME_PERIOD;
    ;
    dbStore = H2DbStore.getOrCreateStore(db);
  }

  public void computeComplement(Set<byte[]> a, Set<byte[]> b) {
    Iterator<byte[]> iterator = b.iterator();
    while (iterator.hasNext()) {
      byte[] e = iterator.next();
      if (a.contains(e)) {
        a.remove(e);
        iterator.remove();
      }
    }
  }

  public Set<byte[]> getDigestBlock(long ts) {
    return dbStore.getDigestRange(ts - BLOCK_TIME_PERIOD, ts);
  }

  @Override
  public void runOneIteration() throws Exception {
    long current = currentTS;

    while (current > _lastVerifiedTS.get()) {
      Set<byte[]> a = getDigestBlock(current);
      ms.sendBlock(a);

      Set<byte[]> b = null;
      try {
        b = ms.getBlock();
      } catch (InterruptedException e) {
      }

      computeComplement(a, b);

      if (blockCache.containsKey(current)) {
        blockCache.get(current).addAll(b);
      } else {
        blockCache.put(current, b);
      }

      current -= BLOCK_TIME_PERIOD;
    }
  }

  public void flushCache(String id) { // blocking queue
    for (long k : blockCache.keySet()) {
      Set<byte[]> hs = blockCache.get(k);
      // TODO
      // request messages with the following digests in hs
      // save them to database
      System.out.println("set diff for id = " + id + " " + Arrays.toString(
              hs.stream().map(s -> new String(s, StandardCharsets.UTF_8)).sorted().toArray()));
    }

    _lastVerifiedTS.set(currentTS);
    System.out.println("Verified TS After: " + _lastVerifiedTS.get());
    blockCache.clear();
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedRateSchedule(1500, 1000, TimeUnit.MILLISECONDS);
  }
}
