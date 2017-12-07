package ramble.messagesync.netty;

import com.google.protobuf.ByteString;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.log4j.Logger;
import ramble.api.MessageSyncProtocol;
import ramble.api.RambleMessage;
import ramble.messagesync.api.MessageSyncClient;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class NettyMessageSyncClient implements MessageSyncClient {

  private static final Logger LOG = Logger.getLogger(NettyMessageSyncClient.class);

  private final String host;
  private final int port;
  private final EventLoopGroup group;

  private Channel channel;

  public NettyMessageSyncClient(String host, int port) {
    this.host = host;
    this.port = port;
    this.group = new NioEventLoopGroup();
  }

  @Override
  public void connect() throws InterruptedException {
    Bootstrap bootstrap = new Bootstrap();
    bootstrap.group(this.group)
            .channel(NioSocketChannel.class)
            .handler(new NettyMessageSyncClientInitializer());

    LOG.info("Netty client connecting to host " + this.host + " port " + this.port);
    this.channel = bootstrap.connect(this.host, this.port).sync().channel();
  }

  @Override
  public Set<RambleMessage.SignedMessage> syncMessages() {
    NettyMessageSyncClientHandler handle = this.channel.pipeline().get(NettyMessageSyncClientHandler.class);
    MessageSyncProtocol.Response resp = handle.sendRequest(MessageSyncProtocol.Request.newBuilder().setGetAllMessages(
            MessageSyncProtocol.GetAllMessages.newBuilder()).build());
    return new HashSet<>(resp.getSendMessage().getMessages().getSignedMessageList());
  }

  /**
   * Get a {@link Set} of {@link RambleMessage.SignedMessage}s from the remote server that matches the specified
   * {@link Set} of message digests.
   */
  Set<RambleMessage.SignedMessage> getMessages(Set<byte[]> messageDigests) {
    NettyMessageSyncClientHandler handle = this.channel.pipeline().get(NettyMessageSyncClientHandler.class);

    Set<ByteString> messageByteStrings = messageDigests.stream().map(ByteString::copyFrom).collect(Collectors.toSet());

    MessageSyncProtocol.GetMessages getMessagesRequest = MessageSyncProtocol.GetMessages.newBuilder()
            .addAllMessageDigest(messageByteStrings)
            .build();

    MessageSyncProtocol.Request request = MessageSyncProtocol.Request.newBuilder()
            .setGetMessages(getMessagesRequest)
            .build();

    return new HashSet<>(handle.sendRequest(request).getSendMessage().getMessages().getSignedMessageList());
  }

  @Override
  public void disconnect() {
    if (this.channel != null) {
      this.channel.close();
    }
    this.group.shutdownGracefully();
  }
}
