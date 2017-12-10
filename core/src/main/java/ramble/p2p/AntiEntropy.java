package ramble.p2p;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.Service;
import ramble.db.h2.H2DbStore;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.BlockingQueue;

public class AntiEntropy extends AbstractScheduledService implements Service {

  private static final long BLOCK_TIME_PERIOD = 300000; // 5 mins
  private static AtomicLong _lastVerifiedTS = new AtomicLong(0);

  private final H2DbStore dbStore;
  private long currentTS;
  private HashSet<byte[]> blockCache = new HashSet<>();
  private BlockingQueue<byte[]> queue;
  private LocalMessageService ms;

  public AntiEntropy(LocalMessageService ms, String db, long ts, long nts) {
    if (_lastVerifiedTS.get() < ts)
      _lastVerifiedTS.set(ts);

    System.out.println("Verified TS Before: " + _lastVerifiedTS.get());
    this.ms = ms;
    currentTS = (nts / BLOCK_TIME_PERIOD) * BLOCK_TIME_PERIOD;
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
    long end = _lastVerifiedTS.get();

    ms.sendEndTS(end);
    end = Math.min(end, ms.getEndTS());

    while (current > end) {
      Set<byte[]> a = getDigestBlock(current);
      ms.sendBlock(a);

      Set<byte[]> b = null;
      try {
        b = ms.getBlock();
      } catch (InterruptedException e) {
      }

      computeComplement(a, b);

      for (byte[] s : b) {
        if (blockCache.add(s)) {
          queue.put(s);
        }
      }

      byte[] sentinel = ("END:" + current).getBytes();
      queue.put(sentinel);
      current -= BLOCK_TIME_PERIOD;
    }
  }

  public void flushCache() {
    blockCache.clear();
  }

  public void updateVerifiedTS(long ts) {
    _lastVerifiedTS.set(ts);
  }

  public byte[] getOne() {
    byte[] digest = null;

    try{
      if(!queue.isEmpty()){
        return queue.take();
      }
    }catch(InterruptedException e) {
      e.printStackTrace();
    }

    return null;
  }

  private void enqueue(byte[] digest) {
    try {
      queue.put(digest);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedRateSchedule(1500, 1000, TimeUnit.MILLISECONDS);
  }
}
