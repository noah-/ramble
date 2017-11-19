package ramble.db.mem;

import ramble.api.RambleMessage;
import ramble.db.api.DbStore;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class InMemoryDbStore implements DbStore {

  private final Map<String, RambleMessage.SignedMessage> messages = new HashMap<>();

  @Override
  public boolean exists(RambleMessage.SignedMessage message) {
    return this.messages.containsKey(message.getMessage().getMessageDigest().toString(StandardCharsets.UTF_8));
  }

  @Override
  public void store(RambleMessage.SignedMessage message) {
    this.messages.put(message.getMessage().getMessageDigest().toString(StandardCharsets.UTF_8), message);
  }

  @Override
  public RambleMessage.SignedMessage get(String id) {
    return this.messages.get(id);
  }
}
