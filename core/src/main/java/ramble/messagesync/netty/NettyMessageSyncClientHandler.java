package ramble.messagesync.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.log4j.Logger;
import ramble.api.MessageSyncProtocol;
import ramble.messagesync.api.MessageSyncClient;
import ramble.messagesync.api.MessageClientSyncHandler;

public class NettyMessageSyncClientHandler extends SimpleChannelInboundHandler<MessageSyncProtocol.Response> {

  private static final Logger LOG = Logger.getLogger(NettyMessageSyncClient.class);

  private final MessageSyncClient messageSyncClient;
  private final MessageClientSyncHandler messageClientSyncHandler;

  NettyMessageSyncClientHandler(MessageSyncClient messageSyncClient, MessageClientSyncHandler messageClientSyncHandler) {
    this.messageSyncClient = messageSyncClient;
    this.messageClientSyncHandler = messageClientSyncHandler;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, MessageSyncProtocol.Response msg) {
    this.messageClientSyncHandler.handleResponse(this.messageSyncClient, msg);
    ctx.close();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOG.error("Caught exception on Netty channel, closing connection", cause);
    ctx.close();
  }
}
