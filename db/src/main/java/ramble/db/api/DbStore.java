package ramble.db.api;

import ramble.api.RambleMessage;

public interface DbStore {

  boolean exists(RambleMessage.SignedMessage message);

  void store(RambleMessage.SignedMessage message);

  RambleMessage.SignedMessage get(String id);
}
