package ramble.messagesync;

import ramble.db.api.DbStore;
import ramble.messagesync.api.MessageSyncServer;
import ramble.messagesync.netty.NettyMessageSyncServer;

public class MessageSyncServerFactory {

  public static MessageSyncServer getMessageSyncServer(DbStore dbStore, int port) {
    return new NettyMessageSyncServer(dbStore, port);
  }
}
