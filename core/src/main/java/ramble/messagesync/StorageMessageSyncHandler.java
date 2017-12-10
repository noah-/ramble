package ramble.messagesync;

import org.apache.log4j.Logger;
import ramble.api.MessageSyncProtocol;
import ramble.api.RambleMessage;
import ramble.crypto.MessageSigner;
import ramble.db.DbStoreFactory;
import ramble.db.api.DbStore;
import ramble.messagesync.api.MessageSyncClient;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;


public class StorageMessageSyncHandler extends TypedMessageSyncHandler {

  private static final Logger LOG = Logger.getLogger(StorageMessageSyncHandler.class);

  private final String id;
  private final DbStore dbStore;

  public StorageMessageSyncHandler(String id) {
    this.id = id;
    this.dbStore = DbStoreFactory.getDbStore(id);
  }

  @Override
  public void handleSendMessagesResponse(MessageSyncClient messageSyncClient, MessageSyncProtocol.SendMessages sendMessagesResponse) {
    try {
      List<RambleMessage.SignedMessage> messages = sendMessagesResponse.getMessages().getSignedMessageList();
      for (RambleMessage.SignedMessage signedMessage : messages) {
        if (MessageSigner.verify(signedMessage.getPublicKey().toByteArray(),
                signedMessage.getMessage().toByteArray(), signedMessage.getSignature().toByteArray())) {

          if (!this.dbStore.exists(signedMessage)) {
            LOG.info("[id = " + this.id + "] Message-sync got new message from " +
                    signedMessage.getMessage().getSourceId() + " - " + signedMessage.getMessage().getMessage());
            this.dbStore.storeIfNotExists(signedMessage);
          }
        }
      }
    } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | InvalidKeySpecException e) {
      throw new RuntimeException(e);
    } finally {
      messageSyncClient.disconnect();
    }
  }
}
