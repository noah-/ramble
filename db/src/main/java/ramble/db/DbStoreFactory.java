package ramble.db;

import ramble.db.api.DbStore;
import ramble.db.mem.InMemoryDbStore;

public class DbStoreFactory {

  public static DbStore getDbStore() {
    return new InMemoryDbStore();
  }
}
