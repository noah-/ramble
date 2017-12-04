package ramble.db;

import ramble.db.api.DbStore;
import ramble.db.h2.H2DbStore;

public class DbStoreFactory {

  public static DbStore getDbStore(String id) {
    return H2DbStore.getOrCreateStore(id);
  }
}
