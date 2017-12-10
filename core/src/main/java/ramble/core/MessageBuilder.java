package ramble.core;

import com.google.protobuf.ByteString;
import ramble.api.RambleMessage;
import ramble.crypto.MessageSigner;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;

public class MessageBuilder {

  public static RambleMessage.SignedMessage buildSignedMessage(String id, String message, PublicKey publicKey, PrivateKey privateKey) {
    long timestamp = System.currentTimeMillis();
    byte[] digest = generateMessageDigest(id, message, timestamp);

    RambleMessage.Message rambleMessage = RambleMessage.Message.newBuilder()
            .setMessage(message)
            .setTimestamp(timestamp)
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

  private static byte[] generateMessageDigest(String id, String message, long timestamp) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(id.getBytes(StandardCharsets.UTF_8));
      md.update(message.getBytes(StandardCharsets.UTF_8));
      ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
      buffer.putLong(timestamp);
      md.update(buffer.array());
      return md.digest();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
