package ramble.crypto;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;


public class JavaKeyPairGenerator {

  public static void main(String args[]) throws IOException, NoSuchAlgorithmException {
    JavaKeyGenerator keyGenerator = new JavaKeyGenerator(1024);
    keyGenerator.createKeys();

    System.out.println("Creating public-private key pair");
    LocalKeyStore localKeyStore = new LocalKeyStore();
    localKeyStore.putPublicKey("ramble-cli", keyGenerator.getPublicKey());
    localKeyStore.putPrivateKey("ramble-cli", keyGenerator.getPrivateKey());
  }
}
