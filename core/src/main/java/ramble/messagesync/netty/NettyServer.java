package ramble.messagesync.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class NettyServer {

  private final int port;

  public NettyServer(int port) {
    this.port = port;
  }

  public void connect() throws InterruptedException {

    // Create event loop groups. One for incoming connections handling and
    // second for handling actual event by workers
    EventLoopGroup serverGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    try {
      ServerBootstrap bootStrap = new ServerBootstrap();
      bootStrap.group(serverGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new NettyServerInitializer());

      // Bind to port
      bootStrap.bind(this.port).sync().channel().closeFuture().sync();
    } finally {
      serverGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}
