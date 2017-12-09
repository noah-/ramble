package ramble.messagesync.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import ramble.api.MessageSyncProtocol;
import ramble.messagesync.api.MessageSyncClient;
import ramble.messagesync.api.MessageSyncHandler;

public class NettyMessageSyncClientInitializer extends ChannelInitializer<SocketChannel> {

  private final MessageSyncClient messageSyncClient;
  private final MessageSyncHandler messageSyncHandler;

  NettyMessageSyncClientInitializer(MessageSyncClient messageSyncClient, MessageSyncHandler messageSyncHandler) {
    this.messageSyncClient = messageSyncClient;
    this.messageSyncHandler = messageSyncHandler;
  }

  @Override
  protected void initChannel(SocketChannel ch) {
    ChannelPipeline p = ch.pipeline();

    p.addLast(new ProtobufVarint32FrameDecoder());
    p.addLast(new ProtobufDecoder(MessageSyncProtocol.Response.getDefaultInstance()));

    p.addLast(new ProtobufVarint32LengthFieldPrepender());
    p.addLast(new ProtobufEncoder());

    p.addLast(new NettyMessageSyncClientHandler(this.messageSyncClient, this.messageSyncHandler));
  }
}
