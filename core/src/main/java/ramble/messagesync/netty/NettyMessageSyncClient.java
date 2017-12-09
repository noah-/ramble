package ramble.messagesync.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.log4j.Logger;
import ramble.api.MessageSyncProtocol;
import ramble.messagesync.api.MessageSyncClient;
import ramble.messagesync.api.MessageSyncHandler;

public class NettyMessageSyncClient implements MessageSyncClient {

  private static final Logger LOG = Logger.getLogger(NettyMessageSyncClient.class);

  private final String host;
  private final int port;
  private final EventLoopGroup group;
  private final MessageSyncHandler messageSyncHandler;

  private Channel channel;

  public NettyMessageSyncClient(String host, int port, MessageSyncHandler messageSyncHandler) {
    this.host = host;
    this.port = port;
    this.group = new NioEventLoopGroup();
    this.messageSyncHandler = messageSyncHandler;
  }

  @Override
  public void connect() throws InterruptedException {
    Bootstrap bootstrap = new Bootstrap();
    bootstrap.group(this.group)
            .channel(NioSocketChannel.class)
            .handler(new NettyMessageSyncClientInitializer(this, this.messageSyncHandler));

    LOG.info("Netty client connecting to host " + this.host + " port " + this.port);
    this.channel = bootstrap.connect(this.host, this.port).sync().channel();

  }

  @Override
  public void sendRequest(MessageSyncProtocol.Request request) {
     this.channel.writeAndFlush(request);
  }

  @Override
  public void disconnect() {
    if (this.channel != null) {
      this.channel.close();
    }
    this.group.shutdownGracefully();
  }
}
