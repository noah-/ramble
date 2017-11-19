package ramble.gossip.apache;

import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.gossip.manager.GossipCore;
import org.apache.gossip.manager.GossipManager;
import org.apache.gossip.manager.handlers.MessageHandler;
import org.apache.gossip.model.Base;
import org.apache.gossip.model.RambleBulkMessage;
import ramble.api.RambleMessage;
import ramble.crypto.MessageSigner;
import ramble.db.DbStoreFactory;
import ramble.db.api.DbStore;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.BlockingQueue;


public class RambleMessageHandler implements MessageHandler {

  private final DbStore dbStore = DbStoreFactory.getDbStore();
  private final BlockingQueue<RambleMessage.Message> messageQueue;

  public RambleMessageHandler(BlockingQueue<RambleMessage.Message> messageQueue) {
    this.messageQueue = messageQueue;
  }

  @Override
  public boolean invoke(GossipCore gossipCore, GossipManager gossipManager, Base base) {
    RambleMessage.BulkSignedMessage bulkSignedMessage;
    try {
      bulkSignedMessage = RambleMessage.BulkSignedMessage.parseFrom(
              ((RambleBulkMessage) base).getBulkSignedMessage());
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }

    for (RambleMessage.SignedMessage signedMessage : bulkSignedMessage.getSignedMessageList()) {
      try {
        if (MessageSigner.verify(signedMessage.getPublicKey().toByteArray(), signedMessage.getMessage().toByteArray(),
                signedMessage.getSignature().toByteArray())) {
          gossipCore.addRambleMessage(signedMessage);
          if (!this.dbStore.exists(signedMessage)) {
            this.dbStore.store(signedMessage);
            this.messageQueue.put(signedMessage.getMessage());
          }
        }
      } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | SignatureException | InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    return true;
  }
}
