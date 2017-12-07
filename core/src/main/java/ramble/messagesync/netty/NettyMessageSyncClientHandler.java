package ramble.messagesync.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import ramble.api.MessageSyncProtocol;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class NettyMessageSyncClientHandler extends SimpleChannelInboundHandler<MessageSyncProtocol.Response> {

  private Channel channel;
  private BlockingQueue<MessageSyncProtocol.Response> resps = new LinkedBlockingQueue<>();

  // Need to re-write this method and re-implement channelRead0
  MessageSyncProtocol.Response sendRequest(MessageSyncProtocol.Request request) {
    // Send request
    this.channel.writeAndFlush(request);

    // Now wait for response from server
    boolean interrupted = false;
    MessageSyncProtocol.Response resp;
    while (true) {
      try {
        resp = this.resps.take(); // need an ACK
        break;
      } catch (InterruptedException ignore) {
        interrupted = true;
      }
    }

    if (interrupted) {
      Thread.currentThread().interrupt();
    }

    return resp;
  }

  @Override
  public void channelRegistered(ChannelHandlerContext ctx) {
    channel = ctx.channel();
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, MessageSyncProtocol.Response msg) {
    this.resps.add(msg);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }
}
