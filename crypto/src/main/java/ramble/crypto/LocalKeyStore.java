package ramble.crypto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;


/**
 * Utility class for reading and writing key pairs on the local filesystem.
 */
public class LocalKeyStore {

  private static final String KEY_STORE_DIR = "/tmp/ramble-key-store/";
  private static final String PUBLIC_KEY_SUFFIX = ".pub";

  private final KeyFactory keyFactory;

  public LocalKeyStore() throws IOException {
    try {
      this.keyFactory = KeyFactory.getInstance("RSA");
      if (!Files.exists(Paths.get(KEY_STORE_DIR))) {
        Files.createDirectory(Paths.get(KEY_STORE_DIR));
      }
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public void putPublicKey(String id, PublicKey publicKey) throws IOException {
    writeKeyFile(id + PUBLIC_KEY_SUFFIX, publicKey);
  }

  public void putPrivateKey(String id, PrivateKey privateKey) throws IOException {
    writeKeyFile(id, privateKey);
  }

  public PublicKey getPublicKey(String id) throws IOException {
    try {
      return this.keyFactory.generatePublic(new X509EncodedKeySpec(readKeyFile(id + PUBLIC_KEY_SUFFIX)));
    } catch (InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }
  }

  public PrivateKey getPrivateKey(String id) throws IOException {
    try {
      return this.keyFactory.generatePrivate(new PKCS8EncodedKeySpec(readKeyFile(id)));
    } catch (InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean deletePublicKey(String id) throws IOException {
    return deleteKeyFile(id + PUBLIC_KEY_SUFFIX);
  }

  public boolean deletePrivateKey(String id) throws IOException {
    return deleteKeyFile(id);
  }

  private boolean deleteKeyFile(String id) throws IOException {
    return Files.deleteIfExists(Paths.get(KEY_STORE_DIR, id));
  }

  private byte[] readKeyFile(String id) throws IOException {
    return Files.readAllBytes(Paths.get(KEY_STORE_DIR, id));
  }

  private void writeKeyFile(String id, Key key) throws IOException {
    if (!Files.exists(Paths.get(KEY_STORE_DIR, id))) {
      Files.write(Paths.get(KEY_STORE_DIR, id), key.getEncoded(), StandardOpenOption.CREATE_NEW);
    }
  }
}
