package ramble.crypto;

import java.io.IOException;

public class KeyStoreServiceFactory {

  public static KeyStoreService getKeyStoreService() throws KeyServiceException, IOException {
    return new LocalKeyStoreService();
  }
}
