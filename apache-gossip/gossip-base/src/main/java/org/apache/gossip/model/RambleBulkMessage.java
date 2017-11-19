package org.apache.gossip.model;

public class RambleBulkMessage extends Base {

  private byte[] bulkSignedMessage;

  public byte[] getBulkSignedMessage() {
    return this.bulkSignedMessage;
  }

  public void setBulkSignedMessage(byte[] bulkSignedMessage) {
    this.bulkSignedMessage = bulkSignedMessage;
  }
}
