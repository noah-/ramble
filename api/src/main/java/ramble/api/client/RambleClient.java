package ramble.api.client;

import java.util.concurrent.BlockingQueue;


public interface RambleClient {

  /**
   * Posts a given message
   */
  public void post(String message);

  /**
   * Subscribes to a stream of messages
   */
  public BlockingQueue subscribe();
}
