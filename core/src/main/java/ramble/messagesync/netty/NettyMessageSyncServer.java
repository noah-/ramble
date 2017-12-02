package ramble.messagesync.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.apache.log4j.Logger;
import ramble.db.api.DbStore;
import ramble.messagesync.api.MessageSyncServer;

import java.util.concurrent.CompletableFuture;

public class NettyMessageSyncServer implements MessageSyncServer {

  private static final Logger LOG = Logger.getLogger(NettyMessageSyncServer.class);

  private final int port;

  private final EventLoopGroup serverGroup;
  private final EventLoopGroup workerGroup;
  private final ServerBootstrap bootStrap;

  public NettyMessageSyncServer(DbStore dbStore, int port) {
    this.port = port;

    // Create event loop groups. One for incoming connections handling and
    // second for handling actual event by workers
    this.serverGroup = new NioEventLoopGroup(1);
    this.workerGroup = new NioEventLoopGroup();

    this.bootStrap = new ServerBootstrap();
    this.bootStrap.group(serverGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(new NettyMessageSyncServerInitializer(dbStore));
  }

  @Override
  public void start() {
    LOG.info("Starting Message Sync Server on port " + this.port);

    CompletableFuture.runAsync(() -> {
      try {
        bootStrap.bind(this.port).sync().channel().closeFuture().sync();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Override
  public void stop() {
    this.serverGroup.shutdownGracefully();
    this.workerGroup.shutdownGracefully();
  }
}
