package ramble.messagesync.netty;

import ramble.api.RambleMessage;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class NettyClientServerTest {

  public static void main(String args[]) throws InterruptedException, ExecutionException {

    Set<RambleMessage.SignedMessage> messages = new HashSet<>();

    RambleMessage.Message.newBuilder().setMessage("");

//    RambleMessage.SignedMessage.newBuilder().setMessage(messageBuilder);

    CompletableFuture future = CompletableFuture.runAsync(()-> {
      NettyMessageSyncServer nettyServer = new NettyMessageSyncServer(5000, messages);
      try {
        nettyServer.start();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });

    NettyMessageSyncClient nettyClient = new NettyMessageSyncClient("localhost", 5000);
    nettyClient.connect();
    Set<RambleMessage.SignedMessage> messages = nettyClient.syncMessages();

    messages.stream().map(msg -> msg.getMessage().getMessage()).forEach(System.out::println);

    future.get();
  }
}
