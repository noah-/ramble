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
    switch(msg.getResponseTypeCase()) {
      case SENDMESSAGE:
        this.messageSyncHandler.handleSendMessagesResponse(this.messageSyncClient, msg.getSendMessage());
        break;
      case ACK:
        this.messageSyncHandler.handleEmptyResponse(this.messageSyncClient, msg.getAck());
        break;
      case RESPONSETYPE_NOT_SET:
        LOG.error("Received unknown response type in message: " + msg);
        break;
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOG.error("Caught exception on Netty channel, closing connection", cause);
    ctx.close();
  }
}
