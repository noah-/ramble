package org.apache.gossip.model;

public class RambleMessage {

  private final ramble.api.RambleMessage.Message message;

  public RambleMessage(ramble.api.RambleMessage.Message message) {
    this.message = message;
  }

  public ramble.api.RambleMessage.Message getMessage() {
    return message;
  }
}
