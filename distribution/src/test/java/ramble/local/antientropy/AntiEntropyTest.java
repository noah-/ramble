package ramble.local.antientropy;

import com.google.protobuf.ByteString;
import ramble.api.RambleMessage;
import ramble.crypto.JavaKeyGenerator;
import ramble.crypto.MessageSigner;
import ramble.db.DbStoreFactory;
import ramble.db.h2.H2DbStore;
import ramble.p2p.AntiEntropy;
import ramble.p2p.MessageService;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

public class AntiEntropyTest {
    public static RambleMessage.SignedMessage create(JavaKeyGenerator kg, String message, long ts) {
        byte[] digest;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(message.getBytes(StandardCharsets.UTF_8));
            digest = md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        RambleMessage.Message rambleMessage = RambleMessage.Message.newBuilder()
                .setMessage(message)
                .setTimestamp(ts)
                .setSourceId("dead")
                .setMessageDigest(ByteString.copyFrom(digest))
                .build();

        RambleMessage.SignedMessage signedMessage;
        try {
            signedMessage = RambleMessage.SignedMessage.newBuilder()
                    .setMessage(rambleMessage)
                    .setSignature(ByteString.copyFrom(MessageSigner.sign(kg.getPrivateKey(), rambleMessage.toByteArray())))
                    .setPublicKey(ByteString.copyFrom(kg.getPublicKey().getEncoded()))
                    .build();
        } catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        return signedMessage;
    }

    public static void main(String args[]) throws InterruptedException, ExecutionException, SQLException, NoSuchAlgorithmException {

        H2DbStore.getOrCreateStore("ae-test0").runInitializeScripts();
        H2DbStore.getOrCreateStore("ae-test1").runInitializeScripts();

        JavaKeyGenerator kg = new JavaKeyGenerator(1024);
        kg.createKeys();

        RambleMessage.SignedMessage s0 = AntiEntropyTest.create(kg,"Hello", System.currentTimeMillis() - 310000);
        RambleMessage.SignedMessage s1 = AntiEntropyTest.create(kg,"World", System.currentTimeMillis() - 320000);
        RambleMessage.SignedMessage s2 = AntiEntropyTest.create(kg,"Dead", System.currentTimeMillis() - 330000);
        RambleMessage.SignedMessage s3 = AntiEntropyTest.create(kg,"Cow", System.currentTimeMillis() - 340000);
        RambleMessage.SignedMessage s4 = AntiEntropyTest.create(kg,"Beef", System.currentTimeMillis() - 340000);
        RambleMessage.SignedMessage s5 = AntiEntropyTest.create(kg,"OLD", System.currentTimeMillis() - 1100000);

        DbStoreFactory.getDbStore("ae-test0").store(s0);
        DbStoreFactory.getDbStore("ae-test0").store(s1);
        DbStoreFactory.getDbStore("ae-test0").store(s2);
        DbStoreFactory.getDbStore("ae-test0").store(s3);
        DbStoreFactory.getDbStore("ae-test0").store(s5);

        DbStoreFactory.getDbStore("ae-test1").store(s0);
        DbStoreFactory.getDbStore("ae-test1").store(s1);
        DbStoreFactory.getDbStore("ae-test1").store(s2);
        DbStoreFactory.getDbStore("ae-test1").store(s4);

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
    private int n;

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
