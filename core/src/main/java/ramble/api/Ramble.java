package ramble.api;

import ramble.gossip.api.IncomingMessage;

import java.util.concurrent.BlockingQueue;


/**
 * Core interface for interacting with RAMBLE. Any application can start and interact with RAMBLE via this interface.
 */
public interface Ramble {

  /**
   * Start the RAMBLE service
   */
  void start();

  /**
   * Post a message to RAMBLE
   */
  void post(String message);

  /**
   * Listen to messages coming in from other peers on the RAMBLE network. Incoming messages will be added to a
   * {@link BlockingQueue} that applications should query to get any incoming messages.
   */
  BlockingQueue<IncomingMessage> listen();

  /**
   * Shutdown RAMBLE
   */
  void shutdown();
}
