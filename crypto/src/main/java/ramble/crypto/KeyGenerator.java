package ramble.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;


public class KeyGenerator {

  private KeyPairGenerator keyGen;
  private PrivateKey privateKey;
  private PublicKey publicKey;

  public KeyGenerator(int keylength) throws NoSuchAlgorithmException, NoSuchProviderException {
    this.keyGen = KeyPairGenerator.getInstance("RSA");
    this.keyGen.initialize(keylength);
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
