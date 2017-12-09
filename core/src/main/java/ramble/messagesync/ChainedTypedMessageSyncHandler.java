package ramble.messagesync;

import ramble.api.MessageSyncProtocol;
import ramble.messagesync.api.MessageSyncClient;
import ramble.messagesync.api.MessageSyncHandler;

import java.util.Set;

public class ChainedTypedMessageSyncHandler extends TypedMessageSyncHandler implements MessageSyncHandler {

  private final Set<TypedMessageSyncHandler> handlers;

  public ChainedTypedMessageSyncHandler(Set<TypedMessageSyncHandler> handlers) {
    this.handlers = handlers;
  }

  @Override
  protected void handleSendMessagesResponse(MessageSyncClient messageSyncClient,
                                          MessageSyncProtocol.SendMessages sendMessage) {
    this.handlers.forEach(handler -> handler.handleSendMessagesResponse(messageSyncClient, sendMessage));
  }

  @Override
  protected void handleEmptyResponse(MessageSyncClient messageSyncClient, MessageSyncProtocol.Ack ack) {
    this.handlers.forEach(handler -> handler.handleEmptyResponse(messageSyncClient, ack));
  }
}
