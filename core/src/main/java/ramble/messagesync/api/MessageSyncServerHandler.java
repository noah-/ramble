package ramble.messagesync.api;

import ramble.api.MessageSyncProtocol;

public interface MessageSyncServerHandler {

  MessageSyncProtocol.Response handleRequest(MessageSyncProtocol.Request request);
}
