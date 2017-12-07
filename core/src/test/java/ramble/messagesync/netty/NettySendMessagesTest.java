package ramble.messagesync.netty;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.ByteString;
import org.apache.log4j.Logger;
import ramble.api.RambleMessage;
import ramble.crypto.JavaKeyGenerator;
import ramble.crypto.MessageSigner;
import ramble.db.DbStoreFactory;
import ramble.db.h2.H2DbStore;
import ramble.messagesync.MessageSyncServerFactory;
import ramble.messagesync.api.MessageSyncServer;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class NettySendMessagesTest {

  private static final Logger LOG = Logger.getLogger(NettySendMessagesTest.class);

  public static void main(String args[]) throws InterruptedException, ExecutionException, SQLException {

    H2DbStore db = H2DbStore.getOrCreateStore("netty-test");
    db.runInitializeScripts();

    RambleMessage.SignedMessage msg1 = makeMessage("hello");

    RambleMessage.SignedMessage msg2 = makeMessage("world");

    Set<RambleMessage.SignedMessage> messages = ImmutableSet.of(msg1, msg2);

    MessageSyncServer nettyServer = MessageSyncServerFactory.getMessageSyncServer(
            DbStoreFactory.getDbStore("netty-test"), 6000);

    CompletableFuture future = CompletableFuture.runAsync(()-> {
      try {
        nettyServer.start();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });

    for (int i = 0; i < 1; i++) {
      NettyMessageSyncClient nettyClient = new NettyMessageSyncClient("localhost", 6000);
      nettyClient.connect();
      Thread.sleep(1000);
      nettyClient.sendMessage(messages);
      Thread.sleep(1000);
//      nettyClient.disconnect();
      Thread.sleep(1000);
    }

    db.getAllMessages().forEach(System.out::println);

    nettyServer.stop();
    future.cancel(true);
  }

  private static JavaKeyGenerator javaKeyGenerator;

  static {
    try {
      javaKeyGenerator = new JavaKeyGenerator(1024);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private static PublicKey puk;
  private static PrivateKey prk;

  static {
    javaKeyGenerator.createKeys();
    puk = javaKeyGenerator.getPublicKey();
    prk = javaKeyGenerator.getPrivateKey();
  }

  private static RambleMessage.SignedMessage makeMessage(String message) {
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
            .setTimestamp(System.currentTimeMillis())
            .setSourceId("test")
            .setMessageDigest(ByteString.copyFrom(digest))
            .build();

    RambleMessage.SignedMessage signedMessage;
    try {
      signedMessage = RambleMessage.SignedMessage.newBuilder()
              .setMessage(rambleMessage)
              .setSignature(ByteString.copyFrom(MessageSigner.sign(prk, rambleMessage.toByteArray())))
              .setPublicKey(ByteString.copyFrom(puk.getEncoded()))
              .build();
    } catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    return signedMessage;
  }
}
