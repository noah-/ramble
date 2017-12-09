package ramble.messagesync.api;

import ramble.api.MessageSyncProtocol;

public interface MessageSyncClient {

  /**
   * Connects to the remote node with which the message sync protocol will be run
   */
  void connect() throws InterruptedException;

  void sendRequest(MessageSyncProtocol.Request request);

  void disconnect();
}
