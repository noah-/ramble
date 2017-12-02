package ramble.messagesync.netty;

import ramble.api.RambleMessage;
import ramble.db.DbStoreFactory;
import ramble.db.persistent.PersistentDbStore;
import ramble.messagesync.MessageSyncClientFactory;
import ramble.messagesync.MessageSyncServerFactory;
import ramble.messagesync.api.MessageSyncClient;
import ramble.messagesync.api.MessageSyncServer;

import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class NettyClientServerTest {

  public static void main(String args[]) throws InterruptedException, ExecutionException, SQLException {

    PersistentDbStore.getOrCreateStore("netty-test").runInitializeScripts();

    DbStoreFactory.getDbStore("netty-test").store(RambleMessage.SignedMessage.newBuilder().setMessage(
            RambleMessage.Message.newBuilder().setMessage("hello")).build());

    DbStoreFactory.getDbStore("netty-test").store(RambleMessage.SignedMessage.newBuilder().setMessage(
            RambleMessage.Message.newBuilder().setMessage("world")).build());

//    NettyMessageSyncServer nettyServer = new NettyMessageSyncServer(6000);
    MessageSyncServer nettyServer = MessageSyncServerFactory.getMessageSyncServer(
            DbStoreFactory.getDbStore("netty-test"), 6000);

    CompletableFuture future = CompletableFuture.runAsync(()-> {
      try {
        nettyServer.start();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });

    for (int i = 0; i < 10; i++) {
      MessageSyncClient nettyClient = MessageSyncClientFactory.getMessageSyncClient("localhost", 6000);
//      NettyMessageSyncClient nettyClient = new NettyMessageSyncClient("localhost", 6000);
      nettyClient.connect();
      Thread.sleep(1000);
      Set<RambleMessage.SignedMessage> messages = nettyClient.syncMessages();
      messages.forEach(System.out::println);
      Set<RambleMessage.SignedMessage> messages2 = nettyClient.syncMessages();
      messages2.forEach(System.out::println);
      Thread.sleep(1000);
//      nettyClient.shutdown();
      Thread.sleep(1000);
    }

    nettyServer.stop();
    future.get();
  }
}
