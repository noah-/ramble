package ramble.crypto;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;

/**
 * A {@link KeyService} that stores keys on the local filesystem
 */
class LocalKeyService implements KeyService {

  private final String keyStore;
  private final int keylength;

  private KeyPairGenerator keyGen;
  private PrivateKey privateKey;
  private PublicKey publicKey;

  LocalKeyService(int keylength, String keyStore) {
    this.keylength = keylength;
    this.keyStore = keyStore;
  }

  @Override
  public void init() throws KeyServiceException {
    try {
      this.keyGen = KeyPairGenerator.getInstance("RSA");
    } catch (NoSuchAlgorithmException e) {
      throw new KeyServiceException(e);
    }
    this.keyGen.initialize(keylength);
  }

  @Override
  public void createKeys() {
    KeyPair pair = this.keyGen.generateKeyPair();
    this.privateKey = pair.getPrivate();
    this.publicKey = pair.getPublic();
  }

  @Override
  public byte[] getPrivateKey() {
    return this.privateKey.getEncoded();
  }

  @Override
  public byte[] getPublicKey() {
    return this.publicKey.getEncoded();
  }

  @Override
  public String storeKey(byte[] key) throws IOException {
    String id = UUID.randomUUID().toString();
    Files.write(Paths.get(keyStore, id), key, StandardOpenOption.CREATE_NEW);
    return id;
  }

  @Override
  public byte[] getKey(String id) throws IOException {
    return Files.readAllBytes(Paths.get(keyStore, id));
  }
}
