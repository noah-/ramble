package ramble.gossip.api;


/**
 * An incoming message from the Gossip network
 */
public class IncomingMessage {

  private final String senderId;
  private final String message;

  public IncomingMessage(String senderId, String message) {
    this.senderId = senderId;
    this.message = message;
  }

  public String getSenderId() {
    return senderId;
  }

  public String getMessage() {
    return message;
  }
}
