package ramble.messagesync;

import ramble.messagesync.api.MessageSyncServer;
import ramble.messagesync.api.MessageSyncServerHandler;
import ramble.messagesync.netty.NettyMessageSyncServer;

public class MessageSyncServerFactory {

  public static MessageSyncServer getMessageSyncServer(MessageSyncServerHandler messageSyncServerHandler, int port) {
    return new NettyMessageSyncServer(messageSyncServerHandler, port);
  }
}
