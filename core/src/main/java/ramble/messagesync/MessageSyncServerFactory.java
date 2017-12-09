package ramble.messagesync;

import ramble.api.Ramble;
import ramble.db.api.DbStore;
import ramble.messagesync.api.MessageSyncServer;
import ramble.messagesync.netty.NettyMessageSyncServer;

public class MessageSyncServerFactory {

  public static MessageSyncServer getMessageSyncServer(DbStore dbStore, int port, Ramble ramble) {
    return new NettyMessageSyncServer(dbStore, port, ramble);
  }
}
