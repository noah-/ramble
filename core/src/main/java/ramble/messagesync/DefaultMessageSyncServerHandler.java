package ramble.messagesync;

import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;
import org.apache.log4j.Logger;
import ramble.api.MessageSyncProtocol;
import ramble.api.Ramble;
import ramble.api.RambleMessage;
import ramble.crypto.MessageSigner;
import ramble.db.BlockInfo;
import ramble.db.api.DbStore;
import ramble.messagesync.api.MessageSyncServerHandler;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultMessageSyncServerHandler implements MessageSyncServerHandler {

  private static final Logger LOG = Logger.getLogger(DefaultMessageSyncServerHandler.class);

  private final Ramble ramble;
  private final DbStore dbStore;

  public DefaultMessageSyncServerHandler(Ramble ramble, DbStore dbStore) {
    this.ramble = ramble;
    this.dbStore = dbStore;
  }

  @Override
  public MessageSyncProtocol.Response handleRequest(MessageSyncProtocol.Request request) {
    switch (request.getRequestTypeCase()) {
      case GETALLMESSAGES:
        return handleGetAllMessagesRequest();
      case GETMESSAGES:
        return handleGetMessagesRequest(request.getGetMessages());
      case BROADCASTMESSAGES:
        return handleSendMessagesRequest(request.getBroadcastMessages());
      case GETCOMPLEMENT:
        return handleGetComplementRequest(request.getGetComplement());
      case REQUESTTYPE_NOT_SET:
        LOG.error("Got a message with an invalid request type, dropping message");
        return null;
      default:
        LOG.error("Got a message with an invalid request type, dropping message");
        return null;
    }
  }

  private MessageSyncProtocol.Response handleGetComplementRequest(MessageSyncProtocol.GetComplement getComplement) {
    MessageSyncProtocol.Response.Builder responseBuilder = MessageSyncProtocol.Response.newBuilder();
    Set<byte[]> localBlock = this.dbStore.getDigestRange(getComplement.getBlockStartTime(),
            getComplement.getBlockEndTime());

    Set<byte[]> complement = computeComplement(localBlock, getComplement
            .getMessageDigestList()
            .stream()
            .map(ByteString::toByteArray)
            .collect(Collectors.toSet()));

    MessageSyncProtocol.SendMessageDigests sendMessageDigests = MessageSyncProtocol.SendMessageDigests.newBuilder()
            .addAllMessageDigest(complement
                    .stream()
                    .map(ByteString::copyFrom)
                    .collect(Collectors.toSet()))
            .build();

    return responseBuilder.setSendMessageDigests(sendMessageDigests).build();
  }

  private Set<byte[]> computeComplement(Set<byte[]> localBlock, Set<byte[]> remoteBlock) {
    return Sets.difference(localBlock, remoteBlock);
  }

  private MessageSyncProtocol.Response handleSendMessagesRequest(
          MessageSyncProtocol.BroadcastMessages broadcastMessages) {
    try {
      // Verify messages have valid signatures and store them in the DB
      if (MessageSigner.verify(broadcastMessages.getMessages().getSignedMessageList())) {
        for (RambleMessage.SignedMessage signedMessage : broadcastMessages.getMessages().getSignedMessageList()) {
          if (!this.dbStore.exists(signedMessage)) {
            this.dbStore.storeIfNotExists(signedMessage);
            LOG.info("[id = " + ramble.getId() + "] Received broadcasted message and it does not exist locally so " +
                    "will re-broadcast it: " + signedMessage.getMessage().getMessage());
            this.ramble.broadcast(signedMessage);
          } else {
            LOG.info("[id = " + ramble.getId() + "] Received broadcasted message but it exists locally so dropping it: "
                    + signedMessage.getMessage().getMessage());
          }
        }
      } else {
        LOG.error("Verification of signature for message " + broadcastMessages + " failed");
      }
    } catch (Exception e) {
      LOG.error("Error while verifying signatures for messages:\n" + broadcastMessages.getMessages(), e);
    }
    return MessageSyncProtocol.Response.newBuilder().setAck(MessageSyncProtocol.Ack.getDefaultInstance()).build();
  }

  private MessageSyncProtocol.Response handleGetMessagesRequest(MessageSyncProtocol.GetMessages getMessagesRequest) {
    MessageSyncProtocol.Response.Builder responseBuilder = MessageSyncProtocol.Response.newBuilder();

    Set<RambleMessage.SignedMessage> messages = new HashSet<>();
    getMessagesRequest.getMessageDigestList().forEach(
            digest -> messages.addAll(this.dbStore.getMessages(digest.toByteArray())));

    RambleMessage.BulkSignedMessage bulkMessage = RambleMessage.BulkSignedMessage.newBuilder()
            .addAllSignedMessage(messages)
            .build();

    return responseBuilder.setSendMessage(
            MessageSyncProtocol.SendMessages.newBuilder().setMessages(bulkMessage)).build();
  }

  private MessageSyncProtocol.Response handleGetAllMessagesRequest() {
    MessageSyncProtocol.Response.Builder responseBuilder = MessageSyncProtocol.Response.newBuilder();

    AbstractMap.SimpleEntry<Set<RambleMessage.SignedMessage>, Set<BlockInfo>> messagesAndBlockConf =
            this.dbStore.getAllMessagesAndBlockConf();

    RambleMessage.BulkSignedMessage bulkMessage = RambleMessage.BulkSignedMessage.newBuilder()
            .addAllSignedMessage(messagesAndBlockConf.getKey())
            .build();

    MessageSyncProtocol.SendAllMessages sendAllMessages = MessageSyncProtocol.SendAllMessages.newBuilder()
            .setMessages(bulkMessage)
            .addAllBlockConf(messagesAndBlockConf.getValue()
                    .stream()
                    .map(blockInfo -> MessageSyncProtocol.BlockConf.newBuilder()
                            .setCount(blockInfo.getCount())
                            .setTimestamp(blockInfo.getTimestamp())
                            .build())
                    .collect(Collectors.toSet()))
            .build();

    return responseBuilder.setSendAllMessages(sendAllMessages).build();
  }
}
