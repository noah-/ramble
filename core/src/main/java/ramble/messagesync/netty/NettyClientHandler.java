package ramble.messagesync.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import ramble.api.MessageSyncProtocol;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class NettyClientHandler extends SimpleChannelInboundHandler<MessageSyncProtocol.Response> {

  private Channel channel;
  private MessageSyncProtocol.Response resp;
  BlockingQueue<MessageSyncProtocol.Response> resps = new LinkedBlockingQueue<>();

  public MessageSyncProtocol.Response sendRequest() {
    MessageSyncProtocol.Request req = MessageSyncProtocol.Request.newBuilder().setRequestMsg("From Client").build();

    // Send request
    channel.writeAndFlush(req);

    // Now wait for response from server
    boolean interrupted = false;
    for (; ; ) {
      try {
        resp = resps.take();
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
  protected void channelRead0(ChannelHandlerContext ctx, MessageSyncProtocol.Response msg)
          throws Exception {
    resps.add(msg);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }
}
