package ramble.gossip.apache;

import org.apache.gossip.model.Base;

import java.net.URI;


public class RamblePayload extends Base {

  private final String message;
  private final URI source;
  private final URI dest;

  public RamblePayload(String message, URI source, URI dest) {
    this.message = message;
    this.source = source;
    this.dest = dest;
  }

  public String getMessage() {
    return message;
  }

  public URI getSource() {
    return source;
  }

  public URI getDest() {
    return dest;
  }
}
