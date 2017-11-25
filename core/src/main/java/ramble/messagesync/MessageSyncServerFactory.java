package ramble.messagesync;

import ramble.api.RambleMessage;
import ramble.messagesync.api.MessageSyncServer;
import ramble.messagesync.netty.NettyMessageSyncServer;

import java.util.Set;

public class MessageSyncServerFactory {

  public MessageSyncServer getMessageSyncServer(int port, Set<RambleMessage.SignedMessage> messages) {
    return new NettyMessageSyncServer(port, messages);
  }
}
