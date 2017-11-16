package org.apache.gossip.model;

public class RambleBulkMessage extends Base {

  private final ramble.api.RambleMessage.BulkMessage.Builder bulkMessage = ramble.api.RambleMessage.BulkMessage.newBuilder();

  public void addMessage(RambleMessage rambleMessage) {
    this.bulkMessage.addMessage(rambleMessage.getMessage());
  }

  public ramble.api.RambleMessage.BulkMessage getBulkMessage() {
    return this.bulkMessage.build();
  }

  @Override
  public String toString() {
    return bulkMessage.build().toString();
  }
}
