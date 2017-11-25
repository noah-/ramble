package ramble.messagesync.api;

import ramble.api.RambleMessage;

import java.util.Set;

public interface MessageSyncClient {

  /**
   * Connects to the remote node with which the message sync protocol will be run
   */
  void connect() throws InterruptedException;

  /**
   * Runs the message sync protocol with the remote node and returns any new {@link RambleMessage.SignedMessage}s
   */
  Set<RambleMessage.SignedMessage> syncMessages();

  void shutdown();
}
