package ramble.crypto;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class MessageSigner {

  private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";

  public static boolean verify(byte[] publicKey, byte[] data,
                               byte[] signature) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, InvalidKeySpecException {
    Signature dsa = Signature.getInstance(SIGNATURE_ALGORITHM);
    dsa.initVerify(KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey)));
    dsa.update(data);
    return dsa.verify(signature);
  }

  public static byte[] sign(PrivateKey privateKey,
                            byte[] data) throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {
    Signature dsa = Signature.getInstance(SIGNATURE_ALGORITHM);
    dsa.initSign(privateKey);
    dsa.update(data);
    return dsa.sign();
  }
}
