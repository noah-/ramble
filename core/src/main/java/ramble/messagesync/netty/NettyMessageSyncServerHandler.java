package ramble.messagesync.netty;

import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.log4j.Logger;
import ramble.api.MessageSyncProtocol;
import ramble.api.RambleMessage;
import ramble.crypto.MessageSigner;
import ramble.db.api.DbStore;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Set;
import java.util.stream.Collectors;

public class NettyMessageSyncServerHandler extends SimpleChannelInboundHandler<MessageSyncProtocol.Request> {

  private static final Logger LOG = Logger.getLogger(NettyMessageSyncServerHandler.class);

  private DbStore dbStore;

  NettyMessageSyncServerHandler(DbStore dbStore) {
    this.dbStore = dbStore;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, MessageSyncProtocol.Request msg) {
    switch(msg.getRequestTypeCase()) {
      case GETALLMESSAGES:
        ctx.write(handleGetAllMessagesRequest());
        break;
      case GETMESSAGES:
        ctx.write(handleGetMessagesRequest(msg.getGetMessages()));
        break;
      case BROADCASTMESSAGES:
        ctx.write(handleSendMessagesRequest(msg.getBroadcastMessages()));
        break;
      case GETCOMPLEMENT:
        ctx.write(handleGetComplementRequest(msg.getGetComplement()));
      case REQUESTTYPE_NOT_SET:
        LOG.error("Got a message with an invalid request type, dropping message");
        break;
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

  private MessageSyncProtocol.Response handleSendMessagesRequest(MessageSyncProtocol.BroadcastMessages broadcastMessages) {
    try {
      // Verify messages have valid signatures and store them in the DB
      if (MessageSigner.verify(broadcastMessages.getMessages().getSignedMessageList())) {
        this.dbStore.store(broadcastMessages.getMessages());
      }
    } catch (InvalidKeySpecException | NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
      LOG.error("Error while verifying signatures for messages:\n" + broadcastMessages.getMessages(), e);
    }
    return MessageSyncProtocol.Response.newBuilder().setAck(MessageSyncProtocol.Ack.getDefaultInstance()).build();
  }

  private MessageSyncProtocol.Response handleGetMessagesRequest(MessageSyncProtocol.GetMessages getMessagesRequest) {
    MessageSyncProtocol.Response.Builder responseBuilder = MessageSyncProtocol.Response.newBuilder();

    Set<RambleMessage.SignedMessage> messages = this.dbStore.getMessages(getMessagesRequest.getMessageDigestList()
            .stream()
            .map(ByteString::toByteArray)
            .collect(Collectors.toSet()));

    RambleMessage.BulkSignedMessage bulkMessage = RambleMessage.BulkSignedMessage.newBuilder()
            .addAllSignedMessage(messages)
            .build();

    return responseBuilder.setSendMessage(
            MessageSyncProtocol.SendMessages.newBuilder().setMessages(bulkMessage)).build();
  }

  private MessageSyncProtocol.Response handleGetAllMessagesRequest() {
    MessageSyncProtocol.Response.Builder responseBuilder = MessageSyncProtocol.Response.newBuilder();

    RambleMessage.BulkSignedMessage bulkMessage = RambleMessage.BulkSignedMessage.newBuilder()
            .addAllSignedMessage(this.dbStore.getAllMessages())
            .build();

    return responseBuilder.setSendMessage(
            MessageSyncProtocol.SendMessages.newBuilder().setMessages(bulkMessage)).build();
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }
}
