package ramble.messagesync.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import ramble.api.MessageSyncProtocol;

public class NettyClient {

  private final String host;
  private final int port;

  public NettyClient(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public void send() throws InterruptedException {
    EventLoopGroup group = new NioEventLoopGroup();

    try {
      Bootstrap bootstrap = new Bootstrap();
      bootstrap.group(group)
              .channel(NioSocketChannel.class)
              .handler(new NettyClientInitializer());

      // Create connection
      Channel c = bootstrap.connect(this.host, this.port).sync().channel();

      // Get handle to handler so we can send message
      NettyClientHandler handle = c.pipeline().get(NettyClientHandler.class);
      MessageSyncProtocol.Response resp = handle.sendRequest();
      c.close();

      System.out.println("Got reponse msg from Server: " + resp.getResponseMsg());

    } finally {
      group.shutdownGracefully();
    }
  }
}
