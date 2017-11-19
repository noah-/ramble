package ramble.crypto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class KeyReader {

  private final KeyFactory keyFactory;

  public KeyReader() throws NoSuchAlgorithmException {
    this.keyFactory = KeyFactory.getInstance("RSA");
  }

  public PublicKey getPublicKey(Path file) throws IOException, InvalidKeySpecException {
    return this.keyFactory.generatePublic(new X509EncodedKeySpec(Files.readAllBytes(file)));

  }

  public PrivateKey getPrivateKey(Path file) throws IOException, InvalidKeySpecException {
    return this.keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(file)));
  }
}
