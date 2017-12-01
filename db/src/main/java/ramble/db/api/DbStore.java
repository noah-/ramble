package ramble.db.api;

import ramble.api.RambleMessage;

public interface DbStore {

  /**
   * TODO need a bulk version of this query so that it can be converted into a more optimal SQL query
   */
  boolean exists(RambleMessage.SignedMessage message);

  void store(RambleMessage.SignedMessage message);

  RambleMessage.SignedMessage get(String id);
}
