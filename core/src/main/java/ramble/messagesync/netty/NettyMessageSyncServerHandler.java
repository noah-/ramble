package ramble.messagesync.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import ramble.api.MessageSyncProtocol;
import ramble.api.RambleMessage;

import java.util.Set;

public class NettyMessageSyncServerHandler extends SimpleChannelInboundHandler<MessageSyncProtocol.Request> {

  private final Set<RambleMessage.SignedMessage> messages;

  NettyMessageSyncServerHandler(Set<RambleMessage.SignedMessage> messages) {
    this.messages = messages;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, MessageSyncProtocol.Request msg)
          throws Exception {

    MessageSyncProtocol.Response.Builder responseBuilder = MessageSyncProtocol.Response.newBuilder();

    RambleMessage.BulkSignedMessage bulkMessage = RambleMessage.BulkSignedMessage.newBuilder()
            .addAllSignedMessage(this.messages)
            .build();

    responseBuilder.setMessages(bulkMessage);
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
