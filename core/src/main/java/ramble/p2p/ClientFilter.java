package ramble.p2p;

import ramble.api.RambleMessage;
import ramble.db.FingerPrint;
import ramble.db.h2.H2DbStore;

public class ClientFilter {
    private final H2DbStore dbStore;
    private final int CLIENT_MAX_MSGS_PER_MIN = 3;
    private final long LENGTH_OF_MIN = 60000;
    private final long FILTER_MAX_MSGS = LENGTH_OF_MIN/CLIENT_MAX_MSGS_PER_MIN;
    private final long CLIENT_OLDEST_MSG_ACCEPTED = 43200000;

    public ClientFilter(String db) {
        dbStore = H2DbStore.getOrCreateStore(db);
    }

    public boolean isValidClient(byte[] key) {
        FingerPrint bi = dbStore.getFingerPrint(key);

        if (bi != null) {
            if (bi.count > CLIENT_MAX_MSGS_PER_MIN && (System.currentTimeMillis() - bi.tsStart) < 60000) {
                return false;
            } else if (bi.count > 3) {
                long filter = (System.currentTimeMillis() - bi.tsStart) / CLIENT_MAX_MSGS_PER_MIN;
                if (filter > FILTER_MAX_MSGS) {
                    return false;
                }
            }

            if (System.currentTimeMillis() - bi.tsStart > 60*LENGTH_OF_MIN) {
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
