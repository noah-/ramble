package ramble.local.antientropy;

import ramble.api.RambleMessage;
import ramble.db.DbStoreFactory;
import ramble.db.h2.H2DbStore;
import ramble.p2p.AntiEntropy;
import ramble.p2p.MessageService;

import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

public class AntiEntropyTest {
    public static void main(String args[]) throws InterruptedException, ExecutionException, SQLException {

        H2DbStore.getOrCreateStore("ae-test0").runInitializeScripts();
        H2DbStore.getOrCreateStore("ae-test1").runInitializeScripts();

        DbStoreFactory.getDbStore("ae-test0").store(RambleMessage.SignedMessage.newBuilder().setMessage(
                RambleMessage.Message.newBuilder().setMessage("hello")).build());

        DbStoreFactory.getDbStore("ae-test0").store(RambleMessage.SignedMessage.newBuilder().setMessage(
                RambleMessage.Message.newBuilder().setMessage("world")).build());

        DbStoreFactory.getDbStore("ae-test0").store(RambleMessage.SignedMessage.newBuilder().setMessage(
                RambleMessage.Message.newBuilder().setMessage("dead")).build());

        DbStoreFactory.getDbStore("ae-test0").store(RambleMessage.SignedMessage.newBuilder().setMessage(
                RambleMessage.Message.newBuilder().setMessage("beef")).build());

        DbStoreFactory.getDbStore("ae-test1").store(RambleMessage.SignedMessage.newBuilder().setMessage(
                RambleMessage.Message.newBuilder().setMessage("hello")).build());

        DbStoreFactory.getDbStore("ae-test1").store(RambleMessage.SignedMessage.newBuilder().setMessage(
                RambleMessage.Message.newBuilder().setMessage("world")).build());

        DbStoreFactory.getDbStore("ae-test1").store(RambleMessage.SignedMessage.newBuilder().setMessage(
                RambleMessage.Message.newBuilder().setMessage("dead")).build());

        DbStoreFactory.getDbStore("ae-test1").store(RambleMessage.SignedMessage.newBuilder().setMessage(
                RambleMessage.Message.newBuilder().setMessage("chicken")).build());

        AntiEntropyRunnable ae_test0 = new AntiEntropyRunnable(0,"ae-test0", System.currentTimeMillis());
        AntiEntropyRunnable ae_test1 = new AntiEntropyRunnable(1,"ae-test1", System.currentTimeMillis());

        Thread t0 = new Thread(ae_test0);
        Thread t1 = new Thread(ae_test1);

        t0.start();
        t1.start();
    }
}

class AntiEntropyRunnable implements Runnable {
    private final String id;
    private final long ts;
    private final int n;

    public AntiEntropyRunnable(int n, String id, long ts) {
        this.id = id;
        this.ts = ts;
        this.n = n;
    }

    public void run() {
        long last_ts = ((ts - 300000) / 300000) * 300000;
        MessageService ms = new MessageService(n);
        AntiEntropy ae = new AntiEntropy(ms, id, last_ts, ts);
        ae.run();
        ae.flushCache();
    }
}
