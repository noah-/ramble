package ramble.db;

import ramble.db.api.DbStore;
import ramble.db.persistent.PersistentDbStore;

public class DbStoreFactory {

  public static DbStore getDbStore(String id) {
    return PersistentDbStore.getOrCreateStore(id);
  }
}
