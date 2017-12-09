package ramble.messagesync;

import org.apache.log4j.Logger;
import ramble.api.MessageSyncProtocol;
import ramble.api.RambleMessage;
import ramble.core.RambleImpl;
import ramble.crypto.MessageSigner;
import ramble.db.DbStoreFactory;
import ramble.db.api.DbStore;
import ramble.messagesync.api.MessageSyncClient;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class MessageQueueMessageSyncHandler extends TypedMessageSyncHandler {

  private static final Logger LOG = Logger.getLogger(RambleImpl.class);

  private final BlockingQueue<RambleMessage.Message> messageQueue;
  private final String id;
  private final DbStore dbStore;

  public MessageQueueMessageSyncHandler(BlockingQueue<RambleMessage.Message> messageQueue, String id) {
    this.messageQueue = messageQueue;
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
            this.dbStore.store(signedMessage);
            this.messageQueue.put(signedMessage.getMessage());
          }
        }
      }
    } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | InvalidKeySpecException | InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      messageSyncClient.disconnect();
    }
  }

  @Override
  public void handleEmptyResponse(MessageSyncClient messageSyncClient, MessageSyncProtocol.Ack ackResponse) {
    messageSyncClient.disconnect();
  }
}
