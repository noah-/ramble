package ramble.p2p;

import ramble.api.RambleMessage;
import ramble.db.FingerPrint;
import ramble.db.api.DbStore;

public class ClientFilter {

  private final static int CLIENT_MAX_MSGS_PER_MIN = 600; // set to a high value for testing purposes
  private final static long LENGTH_OF_MIN = 60000;
  private final static long LENGTH_OF_HOUR = LENGTH_OF_MIN * 60;
  private final static long FILTER_MAX_MSGS = LENGTH_OF_MIN / CLIENT_MAX_MSGS_PER_MIN;
  private final static long CLIENT_OLDEST_MSG_ACCEPTED = LENGTH_OF_HOUR * 24;

  private final DbStore dbStore;

  public ClientFilter(DbStore dbStore) {
    this.dbStore = dbStore;
  }

  public boolean isValidClient(byte[] key) {
    FingerPrint bi = dbStore.getFingerPrint(key);

    if (bi != null) {
      if (bi.count > CLIENT_MAX_MSGS_PER_MIN && (System.currentTimeMillis() - bi.tsStart) < LENGTH_OF_MIN) {
        return false;
      } else if (bi.count > CLIENT_MAX_MSGS_PER_MIN) {
        long filter = (System.currentTimeMillis() - bi.tsStart) / CLIENT_MAX_MSGS_PER_MIN;
        if (filter > FILTER_MAX_MSGS) {
          return false;
        }
      }

      if (System.currentTimeMillis() - bi.tsStart > LENGTH_OF_HOUR) {
        dbStore.removeFingerPrint(key);
      }
    }

    return true;
  }

  public boolean isValidMessage(RambleMessage.SignedMessage sm) {
    if (sm.getMessage().getTimestamp() < (System.currentTimeMillis() - CLIENT_OLDEST_MSG_ACCEPTED))
      return false;

    dbStore.updateFingerPrint(sm.getMessage().getSourceId().getBytes(), sm.getPublicKey().toByteArray(),
            sm.getMessage().getTimestamp());

    return true;
  }
}
