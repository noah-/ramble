package ramble.messagesync.api;

import ramble.api.MessageSyncProtocol;

public interface MessageClientSyncHandler {

  void handleResponse(MessageSyncClient messageSyncClient, MessageSyncProtocol.Response response);
}
