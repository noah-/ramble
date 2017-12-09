package ramble.messagesync;

import com.google.protobuf.ByteString;
import ramble.api.MessageSyncProtocol;
import ramble.api.RambleMessage;

import java.util.Set;
import java.util.stream.Collectors;


public class RequestBuilder {

  public static MessageSyncProtocol.Request buildGetAllMessagesRequest() {
    return MessageSyncProtocol.Request.newBuilder()
            .setGetAllMessages(MessageSyncProtocol.GetAllMessages.newBuilder())
            .build();
  }

  public static MessageSyncProtocol.Request buildGetMessagesRequest(Set<byte[]> messageDigests) {
    Set<ByteString> messageByteStrings = messageDigests.stream().map(ByteString::copyFrom).collect(Collectors.toSet());

    MessageSyncProtocol.GetMessages getMessagesRequest = MessageSyncProtocol.GetMessages.newBuilder()
            .addAllMessageDigest(messageByteStrings)
            .build();

    return MessageSyncProtocol.Request.newBuilder()
            .setGetMessages(getMessagesRequest)
            .build();
  }

  public static MessageSyncProtocol.Request buildBroadcastMessagesRequest(Set<RambleMessage.SignedMessage> messages) {
    RambleMessage.BulkSignedMessage bulkMessage = RambleMessage.BulkSignedMessage.newBuilder()
            .addAllSignedMessage(messages)
            .build();

    MessageSyncProtocol.BroadcastMessages broadcastMessages = MessageSyncProtocol.BroadcastMessages.newBuilder()
            .setMessages(bulkMessage)
            .build();

    return MessageSyncProtocol.Request.newBuilder()
            .setBroadcastMessages(broadcastMessages)
            .build();
  }

  public static MessageSyncProtocol.Request buildGetComplementRequest(Set<byte[]> messageDigests, long blockStartTime, long blockEndTime) {
    MessageSyncProtocol.GetComplement getComplement = MessageSyncProtocol.GetComplement.newBuilder()
            .addAllMessageDigest(messageDigests.stream().map(ByteString::copyFrom).collect(Collectors.toList()))
            .setBlockStartTime(blockStartTime)
            .setBlockEndTime(blockEndTime)
            .build();

    return MessageSyncProtocol.Request.newBuilder()
            .setGetComplement(getComplement)
            .build();
  }
}
