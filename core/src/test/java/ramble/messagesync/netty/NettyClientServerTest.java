package ramble.messagesync.netty;

import org.apache.log4j.Logger;
import ramble.api.RambleMessage;
import ramble.db.DbStoreFactory;
import ramble.db.h2.H2DbStore;
import ramble.messagesync.MessageSyncClientFactory;
import ramble.messagesync.MessageSyncServerFactory;
import ramble.messagesync.RequestBuilder;
import ramble.messagesync.StorageMessageSyncHandler;
import ramble.messagesync.api.MessageSyncClient;
import ramble.messagesync.api.MessageSyncServer;

import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class NettyClientServerTest {

  private static final Logger LOG = Logger.getLogger(NettyClientServerTest.class);

  public static void main(String args[]) throws InterruptedException, ExecutionException, SQLException {

    H2DbStore.getOrCreateStore("netty-test").runInitializeScripts();

    DbStoreFactory.getDbStore("netty-test").store(RambleMessage.SignedMessage.newBuilder().setMessage(
            RambleMessage.Message.newBuilder().setMessage("hello")).build());

    DbStoreFactory.getDbStore("netty-test").store(RambleMessage.SignedMessage.newBuilder().setMessage(
            RambleMessage.Message.newBuilder().setMessage("world")).build());

//    NettyMessageSyncServer nettyServer = new NettyMessageSyncServer(6000);
    MessageSyncServer nettyServer = MessageSyncServerFactory.getMessageSyncServer(
            DbStoreFactory.getDbStore("netty-test"), 6000, null);

    CompletableFuture future = CompletableFuture.runAsync(nettyServer::startAsync);

    for (int i = 0; i < 10; i++) {
      MessageSyncClient nettyClient = MessageSyncClientFactory.getMessageSyncClient("localhost", 6000, new StorageMessageSyncHandler("netty-test"));
      nettyClient.connect();
      Thread.sleep(1000);
      nettyClient.sendRequest(RequestBuilder.buildGetAllMessagesRequest());
      Thread.sleep(1000);
//      nettyClient.shutdown();
      Thread.sleep(1000);
    }

    H2DbStore.getOrCreateStore("netty-test").getAllMessages().forEach(System.out::println);

    nettyServer.stopAsync();
    future.get();
  }
}
