package ramble.messagesync.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.log4j.Logger;
import ramble.api.MessageSyncProtocol;
import ramble.messagesync.api.MessageSyncClient;
import ramble.messagesync.api.MessageSyncHandler;

public class NettyMessageSyncClientHandler extends SimpleChannelInboundHandler<MessageSyncProtocol.Response> {

  private static final Logger LOG = Logger.getLogger(NettyMessageSyncClient.class);

  private final MessageSyncClient messageSyncClient;
  private final MessageSyncHandler messageSyncHandler;

  NettyMessageSyncClientHandler(MessageSyncClient messageSyncClient, MessageSyncHandler messageSyncHandler) {
    this.messageSyncClient = messageSyncClient;
    this.messageSyncHandler = messageSyncHandler;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, MessageSyncProtocol.Response msg) {
    this.messageSyncHandler.handleResponse(this.messageSyncClient, msg);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOG.error("Caught exception on Netty channel, closing connection", cause);
    ctx.close();
  }
}
