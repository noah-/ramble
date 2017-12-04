package ramble.api;

import java.net.URI;
import java.util.Set;

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

//  void post(List<String> messages);
//
//  void getLatestMessage();
//
//  void importKey(String key);

  /**
   * Shutdown RAMBLE
   */
  void shutdown();

  String getId();

  Set<RambleMessage.Message> getAllMessages();

  Set<URI> getMembers();
}
