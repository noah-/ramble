package ramble.db;

import ramble.db.api.DbStore;
import ramble.db.persistent.PersistentDbStore;

public class DbStoreFactory {

  public static DbStore getDbStore() {
    return new PersistentDbStore();
  }
}
