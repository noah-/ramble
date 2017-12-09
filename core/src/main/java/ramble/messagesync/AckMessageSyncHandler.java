package ramble.messagesync;

import ramble.api.MessageSyncProtocol;
import ramble.messagesync.api.MessageSyncClient;

public class AckMessageSyncHandler extends TypedMessageSyncHandler {

  @Override
  public void handleEmptyResponse(MessageSyncClient messageSyncClient, MessageSyncProtocol.Ack ackResponse) {
    messageSyncClient.disconnect();
  }
}
