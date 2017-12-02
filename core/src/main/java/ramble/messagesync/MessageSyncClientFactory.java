package ramble.messagesync;

import ramble.messagesync.api.MessageSyncClient;
import ramble.messagesync.netty.NettyMessageSyncClient;

public class MessageSyncClientFactory {

  public static MessageSyncClient getMessageSyncClient(String host, int port) {
    return new NettyMessageSyncClient(host, port);
  }
}
