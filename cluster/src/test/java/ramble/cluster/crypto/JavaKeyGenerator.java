package ramble.cluster.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;


public class JavaKeyGenerator {

  private final KeyPairGenerator keyGen;

  private PrivateKey privateKey;
  private PublicKey publicKey;

  public JavaKeyGenerator(int keyLength) throws NoSuchAlgorithmException {
    this.keyGen = KeyPairGenerator.getInstance("RSA");
    this.keyGen.initialize(keyLength);
  }

  public void createKeys() {
    KeyPair pair = this.keyGen.generateKeyPair();
    this.privateKey = pair.getPrivate();
    this.publicKey = pair.getPublic();
  }

  public PrivateKey getPrivateKey() {
    return this.privateKey;
  }

  public PublicKey getPublicKey() {
    return this.publicKey;
  }

}
