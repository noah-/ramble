package ramble.crypto;


/**
 * A {@link KeyService} that manages keys using AWS's Key Management Service (KMS)
 */
public class AWSCMSService implements KeyService {

  @Override
  public void init() throws KeyServiceException {

  }

  @Override
  public void createKeys() {

  }

  @Override
  public byte[] getPublicKey() {
    return new byte[0];
  }

  @Override
  public byte[] getPrivateKey() {
    return new byte[0];
  }

  @Override
  public String storeKey(byte[] key) {
    // TODO
    return null;
  }

  @Override
  public byte[] getKey(String id) {
    // TODO
    return new byte[0];
  }
}
