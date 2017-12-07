package ramble.messagesync.netty;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.ByteString;
import org.apache.log4j.Logger;
import ramble.api.RambleMessage;
import ramble.db.DbStoreFactory;
import ramble.db.h2.H2DbStore;
import ramble.messagesync.MessageSyncServerFactory;
import ramble.messagesync.api.MessageSyncServer;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class NettyGetMessagesTest {

  private static final Logger LOG = Logger.getLogger(NettyGetMessagesTest.class);

  public static void main(String args[]) throws InterruptedException, ExecutionException, SQLException {

    H2DbStore.getOrCreateStore("netty-test").runInitializeScripts();

    byte[] helloDigest = "hello-digest".getBytes(StandardCharsets.UTF_8);
    byte[] worldDigest = "world-digest".getBytes(StandardCharsets.UTF_8);

    DbStoreFactory.getDbStore("netty-test").store(RambleMessage.SignedMessage.newBuilder()
            .setMessage(RambleMessage.Message.newBuilder()
                    .setMessage("hello")
                    .setMessageDigest(ByteString.copyFrom(helloDigest)))
            .build());

    DbStoreFactory.getDbStore("netty-test").store(RambleMessage.SignedMessage.newBuilder()
            .setMessage(RambleMessage.Message.newBuilder()
                    .setMessage("world")
                    .setMessageDigest(ByteString.copyFrom(worldDigest)))
            .build());

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
      Set<RambleMessage.SignedMessage> messages = nettyClient.getMessages(ImmutableSet.of(helloDigest));
      messages.forEach(LOG::info);
      Set<RambleMessage.SignedMessage> messages2 = nettyClient.getMessages(ImmutableSet.of(worldDigest));
      messages2.forEach(LOG::info);
      Thread.sleep(1000);
//      nettyClient.shutdown();
      Thread.sleep(1000);
    }

    nettyServer.stop();
    future.get();
  }
}
