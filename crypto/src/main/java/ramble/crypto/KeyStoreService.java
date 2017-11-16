package ramble.crypto;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;


public interface KeyStoreService {

  void putPublicKey(String id, PublicKey publicKey) throws IOException;

  void putPrivateKey(String id, PrivateKey privateKey) throws IOException;

  PublicKey getPublicKey(String id) throws IOException, KeyServiceException;

  PrivateKey getPrivateKey(String id) throws IOException, KeyServiceException;

  boolean deletePublicKey(String id) throws IOException;

  boolean deletePrivateKey(String id) throws IOException;
}
