package ramble.messagesync.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import ramble.api.MessageSyncProtocol;
import ramble.api.RambleMessage;
import ramble.messagesync.api.MessageSyncClient;

import java.util.HashSet;
import java.util.Set;

public class NettyMessageSyncClient implements MessageSyncClient {

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

    this.channel = bootstrap.connect(this.host, this.port).sync().channel();
  }

  @Override
  public Set<RambleMessage.SignedMessage> syncMessages() {
    System.out.println("Connecting to host " + this.host + " port " + this.port);
    System.out.println("Is null: " + this.channel);

    NettyMessageSyncClientHandler handle = this.channel.pipeline().get(NettyMessageSyncClientHandler.class);
    MessageSyncProtocol.Response resp = handle.sendRequest(MessageSyncProtocol.Request.newBuilder().setGetAllMessages(
            MessageSyncProtocol.GetAllMessages.newBuilder()).build());
    return new HashSet<>(resp.getSendAllMessage().getMessages().getSignedMessageList());
  }

  @Override
  public void shutdown() {
    this.channel.close();
    this.group.shutdownGracefully();
  }
}
