package ramble.messagesync.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import ramble.api.RambleMessage;
import ramble.messagesync.api.MessageSyncServer;

import java.util.Set;

public class NettyMessageSyncServer implements MessageSyncServer {

  private final int port;

  private final EventLoopGroup serverGroup;
  private final EventLoopGroup workerGroup;
  private final ServerBootstrap bootStrap;

  public NettyMessageSyncServer(int port, Set<RambleMessage.SignedMessage> messages) {
    this.port = port;

    // Create event loop groups. One for incoming connections handling and
    // second for handling actual event by workers
    this.serverGroup = new NioEventLoopGroup(1);
    this.workerGroup = new NioEventLoopGroup();

    this.bootStrap = new ServerBootstrap();
    this.bootStrap.group(serverGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(new NettyMessageSyncServerInitializer(null));
  }

  @Override
  public void start() throws InterruptedException {
    // Bind to port
    bootStrap.bind(this.port).sync().channel().closeFuture().sync();
  }

  @Override
  public void stop() {
    this.serverGroup.shutdownGracefully();
    this.workerGroup.shutdownGracefully();
  }
}
