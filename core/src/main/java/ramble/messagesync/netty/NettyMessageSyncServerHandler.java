package ramble.messagesync.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import ramble.api.MessageSyncProtocol;
import ramble.api.RambleMessage;
import ramble.db.api.DbStore;

public class NettyMessageSyncServerHandler extends SimpleChannelInboundHandler<MessageSyncProtocol.Request> {

  private DbStore dbStore;

  NettyMessageSyncServerHandler(DbStore dbStore) {
    this.dbStore = dbStore;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, MessageSyncProtocol.Request msg) {
    MessageSyncProtocol.Response.Builder responseBuilder = MessageSyncProtocol.Response.newBuilder();

    RambleMessage.BulkSignedMessage bulkMessage = RambleMessage.BulkSignedMessage.newBuilder()
            .addAllSignedMessage(dbStore.getAllMessages())
            .build();

    responseBuilder.setSendAllMessage(MessageSyncProtocol.SendAllMessages.newBuilder().setMessages(bulkMessage));
    ctx.write(responseBuilder.build());
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
