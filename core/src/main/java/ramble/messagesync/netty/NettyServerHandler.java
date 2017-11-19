package ramble.messagesync.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import ramble.api.MessageSyncProtocol;

public class NettyServerHandler extends SimpleChannelInboundHandler<MessageSyncProtocol.Request> {

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, MessageSyncProtocol.Request msg)
          throws Exception {

    MessageSyncProtocol.Response.Builder builder = MessageSyncProtocol.Response.newBuilder();
    builder.setResponseMsg("Accepted from Server, returning response")
            .setRet(0);
    ctx.write(builder.build());
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
