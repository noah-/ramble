package ramble.api;

import ramble.gossip.api.IncomingMessage;

import java.util.concurrent.BlockingQueue;


public interface Ramble {

  void post(String message);

  BlockingQueue<IncomingMessage> listen();
}
