package ramble.db.api;

import ramble.api.MessageSyncProtocol;
import ramble.api.RambleMessage;
import ramble.db.BlockInfo;

import java.util.AbstractMap;
import java.util.Set;


/**
 * Interface for storing {@link RambleMessage.SignedMessage}s in a datastore.
 */
public interface DbStore {

  boolean exists(RambleMessage.SignedMessage message);

  void store(RambleMessage.SignedMessage message);

  void storeIfNotExists(RambleMessage.SignedMessage message);

  void store(RambleMessage.BulkSignedMessage messages);

  Set<byte[]> getDigestRange(long startTimestamp, long endTimestamp);

  Set<RambleMessage.SignedMessage> getRange(long startTimestamp, long endTimestamp);

  Set<RambleMessage.SignedMessage> getAllMessages();

  AbstractMap.SimpleEntry<Set<RambleMessage.SignedMessage>, Set<BlockInfo>> getAllMessagesAndBlockConf();

  Set<RambleMessage.SignedMessage> getMessages(byte[] messageDigest);

  long getLastVerifiedTimestamp(int count);

  void store(MessageSyncProtocol.BlockConf blockConf);
}
