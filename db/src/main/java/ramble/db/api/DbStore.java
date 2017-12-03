package ramble.db.api;

import ramble.api.RambleMessage;

import java.util.Set;


/**
 * Interface for storing {@link RambleMessage.SignedMessage}s in a datastore.
 */
public interface DbStore {

  boolean exists(RambleMessage.SignedMessage message);

  void store(RambleMessage.SignedMessage message);

  Set<RambleMessage.SignedMessage> getRange(long startTimestamp, long endTimestamp);

  Set<RambleMessage.SignedMessage> getAllMessages();
}
