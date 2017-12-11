package ramble.messagesync;

import org.apache.log4j.Logger;
import ramble.api.MessageSyncProtocol;
import ramble.api.RambleMessage;
import ramble.db.api.DbStore;
import ramble.messagesync.api.MessageClientSyncHandler;
import ramble.messagesync.api.MessageSyncClient;

import java.net.URI;
import java.util.concurrent.BlockingQueue;


public class BootstrapProtocol {

  private static final Logger LOG = Logger.getLogger(BootstrapProtocol.class);

  private final String id;
  private final URI target;
  private final BlockingQueue<RambleMessage.Message> messageQueue;
  private final DbStore dbStore;

  public BootstrapProtocol(URI target, String id, DbStore dbStore, BlockingQueue<RambleMessage.Message> messageQueue) {
    this.id = id;
    this.target = target;
    this.dbStore = dbStore;
    this.messageQueue = messageQueue;
  }

  public void run() throws InterruptedException {
    LOG.info("[id = " + this.id + "] Running bootstrap protocol with target " + this.target);

    MessageSyncClient client = MessageSyncClientFactory.getMessageSyncClient(this.id, this.target.getHost(),
            this.target.getPort(), new MessageQueueMessageClientSyncHandler());

    // May need to add an explicit disconnect here in case there is an error while sending the request, but Netty may
    // handle it internally so its ok for now
    client.connect();
    client.sendRequest(RequestBuilder.buildGetAllMessagesRequest());
  }

  public class MessageQueueMessageClientSyncHandler implements MessageClientSyncHandler {

    @Override
    public void handleResponse(MessageSyncClient messageSyncClient, MessageSyncProtocol.Response response) {
      try {
        for (RambleMessage.SignedMessage signedMessage : response.getSendAllMessages().getMessages().getSignedMessageList()) {
          LOG.info("[id = " + id + "] Bootstrap got new message " + signedMessage.getMessage().getMessage());
          dbStore.store(signedMessage);
          messageQueue.put(signedMessage.getMessage());
        }

        for (MessageSyncProtocol.BlockConf blockConf : response.getSendAllMessages().getBlockConfList()) {
          dbStore.store(blockConf);
        }
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      } finally {
        messageSyncClient.disconnect();
      }
    }
  }
}
