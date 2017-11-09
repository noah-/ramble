package ramble.crypto;

public class KeyServiceFactory {

  public static KeyService getKeyService(int keylength, String keyStore) {
    return new LocalKeyService(keylength, keyStore);
  }
}
