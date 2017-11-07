package ramble.gossip.api;


/**
 * An incoming message from the Gossip network
 */
public class IncomingMessage {

  private final String message;

  public IncomingMessage(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
