package ramble.crypto;

import java.io.IOException;

public interface KeyService {

  void init() throws KeyServiceException;

  void createKeys();

  byte[] getPublicKey();

  byte[] getPrivateKey();

  /**
   * Returns the id of the stored key
   */
  String storeKey(byte[] key) throws IOException;

  byte[] getKey(String id) throws IOException;
}
