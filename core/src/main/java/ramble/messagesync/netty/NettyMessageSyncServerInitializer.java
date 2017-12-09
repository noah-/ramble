package ramble.messagesync.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import ramble.api.MessageSyncProtocol;
import ramble.api.Ramble;
import ramble.db.api.DbStore;

public class NettyMessageSyncServerInitializer extends ChannelInitializer<SocketChannel> {

  private final DbStore dbStore;
  private final Ramble ramble;

  NettyMessageSyncServerInitializer(DbStore dbStore, Ramble ramble) {
    this.dbStore = dbStore;
    this.ramble = ramble;
  }

  @Override
  protected void initChannel(SocketChannel ch) {
    ChannelPipeline p = ch.pipeline();
    p.addLast(new ProtobufVarint32FrameDecoder());
    p.addLast(new ProtobufDecoder(MessageSyncProtocol.Request.getDefaultInstance()));

    p.addLast(new ProtobufVarint32LengthFieldPrepender());
    p.addLast(new ProtobufEncoder());

    p.addLast(new NettyMessageSyncServerHandler(this.dbStore, this.ramble));
  }
}
