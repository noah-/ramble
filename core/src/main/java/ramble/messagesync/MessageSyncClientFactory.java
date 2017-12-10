package ramble.messagesync;

import ramble.messagesync.api.MessageSyncClient;
import ramble.messagesync.api.MessageClientSyncHandler;
import ramble.messagesync.netty.NettyMessageSyncClient;

public class MessageSyncClientFactory {

  public static MessageSyncClient getMessageSyncClient(String host, int port, MessageClientSyncHandler messageClientSyncHandler) {
    return new NettyMessageSyncClient(host, port, messageClientSyncHandler);
  }
}
