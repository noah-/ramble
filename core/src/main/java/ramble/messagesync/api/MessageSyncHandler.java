package ramble.messagesync.api;

import ramble.api.MessageSyncProtocol;

public interface MessageSyncHandler {

  void handleResponse(MessageSyncClient messageSyncClient, MessageSyncProtocol.Response response);
}
