package org.apache.gossip.model;

public class RambleBulkMessage extends Base {

  private final byte[] bulkSignedMessage;

  public RambleBulkMessage(byte[] bulkSignedMessage) {
    this.bulkSignedMessage = bulkSignedMessage;
  }

  public byte[] getBulkSignedMessage() {
    return this.bulkSignedMessage;
  }
}
