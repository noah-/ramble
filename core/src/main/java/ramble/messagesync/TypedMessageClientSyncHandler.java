package ramble.messagesync;

import org.apache.log4j.Logger;
import ramble.api.MessageSyncProtocol;
import ramble.messagesync.api.MessageSyncClient;
import ramble.messagesync.api.MessageClientSyncHandler;

public class TypedMessageClientSyncHandler implements MessageClientSyncHandler {

  private static final Logger LOG = Logger.getLogger(TypedMessageClientSyncHandler.class);

  @Override
  public void handleResponse(MessageSyncClient messageSyncClient, MessageSyncProtocol.Response response) {
    switch(response.getResponseTypeCase()) {
      case SENDMESSAGE:
        handleSendMessagesResponse(messageSyncClient, response.getSendMessage());
        break;
      case ACK:
        handleEmptyResponse(messageSyncClient, response.getAck());
        break;
      case RESPONSETYPE_NOT_SET:
        LOG.error("Received unknown response type in response message: " + response);
        break;
    }
  }

  protected void handleSendMessagesResponse(MessageSyncClient messageSyncClient,
                                          MessageSyncProtocol.SendMessages sendMessage) {
    // Do nothing
  }

  protected void handleEmptyResponse(MessageSyncClient messageSyncClient, MessageSyncProtocol.Ack ack) {
    // Do nothing
  }
}
