package ramble.core;

import com.google.protobuf.ByteString;
import ramble.api.RambleMessage;
import ramble.crypto.MessageSigner;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;

public class MessageBuilder {

  public static RambleMessage.SignedMessage buildSignedMessage(String id, String message, PublicKey publicKey, PrivateKey privateKey) {
    byte[] digest = generateMessageDigest(message);

    RambleMessage.Message rambleMessage = RambleMessage.Message.newBuilder()
            .setMessage(message)
            .setTimestamp(System.currentTimeMillis())
            .setSourceId(id)
            .setMessageDigest(ByteString.copyFrom(digest))
            .build();

    try {
      return RambleMessage.SignedMessage.newBuilder()
              .setMessage(rambleMessage)
              .setSignature(ByteString.copyFrom(MessageSigner.sign(privateKey, rambleMessage.toByteArray())))
              .setPublicKey(ByteString.copyFrom(publicKey.getEncoded()))
              .build();
    } catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private static byte[] generateMessageDigest(String message) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(message.getBytes(StandardCharsets.UTF_8));
      return md.digest();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
