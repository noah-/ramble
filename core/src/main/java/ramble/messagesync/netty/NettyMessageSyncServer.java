package ramble.messagesync.netty;

import com.google.common.util.concurrent.AbstractIdleService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.apache.log4j.Logger;
import ramble.api.MessageSyncProtocol;
import ramble.messagesync.api.MessageSyncServer;
import ramble.messagesync.api.MessageSyncServerHandler;

import java.util.concurrent.CompletableFuture;

public class NettyMessageSyncServer extends AbstractIdleService implements MessageSyncServer {

  private static final Logger LOG = Logger.getLogger(NettyMessageSyncServer.class);

  private final int port;
  private final String id;

  private final EventLoopGroup serverGroup;
  private final EventLoopGroup workerGroup;
  private final ServerBootstrap bootStrap;

  public NettyMessageSyncServer(String id, MessageSyncServerHandler messageSyncServerHandler, int port) {
    this.id = id;
    this.port = port;

    // Create event loop groups. One for incoming connections handling and
    // second for handling actual event by workers
    this.serverGroup = new NioEventLoopGroup(1);
    this.workerGroup = new NioEventLoopGroup();

    this.bootStrap = new ServerBootstrap();
    this.bootStrap.group(serverGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(new ChannelInitializer<SocketChannel>() {
              @Override
              protected void initChannel(SocketChannel ch) {
                ch.pipeline().addLast(new ProtobufVarint32FrameDecoder(),
                        new ProtobufDecoder(MessageSyncProtocol.Request.getDefaultInstance()),
                        new ProtobufVarint32LengthFieldPrepender(), new ProtobufEncoder(),
                        new NettyMessageSyncServerHandler(messageSyncServerHandler));
              }
            });
  }

  @Override
  protected void startUp() {
    LOG.info("[id = " + this.id + "] Starting Message Sync Server on port " + this.port);

    // Not sure if running this async is necessary / safe
    CompletableFuture.runAsync(() -> {
      try {
        bootStrap.bind(this.port).sync().channel().closeFuture().sync();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      } finally {
        this.serverGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
      }
    });
  }

  @Override
  protected void shutDown() {
    LOG.info("[id = " + this.id + "] Shutting down Message Sync Server");

    this.serverGroup.shutdownGracefully();
    this.workerGroup.shutdownGracefully();
  }
}
