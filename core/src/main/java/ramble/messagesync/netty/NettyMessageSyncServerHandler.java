package ramble.messagesync.netty;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import ramble.api.MessageSyncProtocol;
import ramble.messagesync.api.MessageSyncServerHandler;

public class NettyMessageSyncServerHandler extends SimpleChannelInboundHandler<MessageSyncProtocol.Request> {

  private final MessageSyncServerHandler messageSyncServerHandler;

  NettyMessageSyncServerHandler(MessageSyncServerHandler messageSyncServerHandler) {
    this.messageSyncServerHandler = messageSyncServerHandler;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, MessageSyncProtocol.Request msg) {
    MessageSyncProtocol.Response response = this.messageSyncServerHandler.handleRequest(msg);
    if (response != null) {
      ctx.write(response).addListener(ChannelFutureListener.CLOSE);
    }
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
