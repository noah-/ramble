package org.apache.gossip.model;

import ramble.api.RambleMessage;

public class RambleSignedMessage {

  private final RambleMessage.SignedMessage signedMessage;

  public RambleSignedMessage(RambleMessage.SignedMessage signedMessage) {
    this.signedMessage = signedMessage;
  }

  public RambleMessage.SignedMessage getSignedMessage() {
    return signedMessage;
  }
}
