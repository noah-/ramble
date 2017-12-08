package ramble.messagesync.api;

import ramble.api.MessageSyncProtocol;

public interface MessageSyncHandler {

  void handleSendMessagesResponse(MessageSyncClient messageSyncClient, MessageSyncProtocol.SendMessages sendMessagesResponse);

  void handleEmptyResponse(MessageSyncClient messageSyncClient, MessageSyncProtocol.Ack ackResponse);
}
