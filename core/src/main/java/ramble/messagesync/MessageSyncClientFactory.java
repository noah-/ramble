package ramble.messagesync;

import ramble.messagesync.api.MessageSyncClient;
import ramble.messagesync.api.MessageClientSyncHandler;
import ramble.messagesync.netty.NettyMessageSyncClient;

public class MessageSyncClientFactory {

  public static MessageSyncClient getMessageSyncClient(String id, String host, int port, MessageClientSyncHandler messageClientSyncHandler) {
    return new NettyMessageSyncClient(id, host, port, messageClientSyncHandler);
  }
}
