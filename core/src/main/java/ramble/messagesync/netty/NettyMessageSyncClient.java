package ramble.messagesync.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import org.apache.log4j.Logger;
import ramble.api.MessageSyncProtocol;
import ramble.messagesync.api.MessageSyncClient;
import ramble.messagesync.api.MessageClientSyncHandler;

public class NettyMessageSyncClient implements MessageSyncClient {

  private static final Logger LOG = Logger.getLogger(NettyMessageSyncClient.class);

  private final String id;
  private final String host;
  private final int port;
  private final EventLoopGroup group;
  private final MessageClientSyncHandler messageClientSyncHandler;

  private Channel channel;

  public NettyMessageSyncClient(String id, String host, int port, MessageClientSyncHandler messageClientSyncHandler) {
    this.id = id;
    this.host = host;
    this.port = port;
    this.group = new NioEventLoopGroup();
    this.messageClientSyncHandler = messageClientSyncHandler;
  }

  @Override
  public void connect() throws InterruptedException {
    Bootstrap bootstrap = new Bootstrap();
    bootstrap.group(this.group)
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<SocketChannel>() {
              @Override
              protected void initChannel(SocketChannel ch) {
                ch.pipeline().addLast(new ProtobufVarint32FrameDecoder(),
                        new ProtobufDecoder(MessageSyncProtocol.Response.getDefaultInstance()),
                        new ProtobufVarint32LengthFieldPrepender(), new ProtobufEncoder(),
                        new NettyMessageSyncClientHandler(NettyMessageSyncClient.this, messageClientSyncHandler));

              }
            });

    LOG.info("[id = " + this.id + "] Netty client connecting to host " + this.host + " port " + this.port);
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
