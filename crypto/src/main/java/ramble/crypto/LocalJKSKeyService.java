package ramble.crypto;

/**
 * A {@link KeyService} that generates keys locally and stores them in a Java jks {@link java.security.KeyStore}.
 */
public class LocalJKSKeyService extends LocalKeyService {

  public LocalJKSKeyService(int keylength, String keyStore) {
    super(keylength, keyStore);
  }
}
