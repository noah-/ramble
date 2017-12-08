package ramble.messagesync;

import ramble.messagesync.api.MessageSyncClient;
import ramble.messagesync.api.MessageSyncHandler;
import ramble.messagesync.netty.NettyMessageSyncClient;

public class MessageSyncClientFactory {

  public static MessageSyncClient getMessageSyncClient(String host, int port, MessageSyncHandler messageSyncHandler) {
    return new NettyMessageSyncClient(host, port, messageSyncHandler);
  }
}
