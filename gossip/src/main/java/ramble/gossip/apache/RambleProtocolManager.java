package ramble.gossip.apache;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.gossip.GossipSettings;
import org.apache.gossip.crdt.CrdtModule;
import org.apache.gossip.model.Base;
import org.apache.gossip.model.RambleBulkMessage;
import org.apache.gossip.model.SignedPayload;
import org.apache.gossip.protocol.ProtocolManager;
import org.apache.log4j.Logger;
import ramble.api.RambleMessage;
import ramble.crypto.KeyServiceException;
import ramble.crypto.KeyStoreService;
import ramble.crypto.KeyStoreServiceFactory;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;


public class RambleProtocolManager implements ProtocolManager {

  // For now just a copy of the JacksonProtocolManager
  // TODO: Implement support for signed payloads - for now just sign actual messages
  // this guarantees that no one can impersonate someone else - no one can lie saying some other use said this
  // TODO: Implement Efficient Set Reconcilliation without Prior Context
  // TODO: use protobuf instead of jackson for object serialization
  // should wrap a JacksonProtocolManager and just use protobuf for RambleMessages
  // TODO: fix this ugly reflection hack in apache-gossip

  private static final Logger LOG = Logger.getLogger(RambleProtocolManager.class);

  private final KeyStoreService keyStoreService;
  private final PrivateKey privateKey;

  private final ObjectMapper objectMapper;
  private final String id;

  /**
   * Required for reflection to work!
   */
  public RambleProtocolManager(GossipSettings settings, String id, MetricRegistry registry) {
    objectMapper = buildObjectMapper(settings);
    this.id = id;
    try {
      this.keyStoreService = KeyStoreServiceFactory.getKeyStoreService();
      this.privateKey = this.keyStoreService.getPrivateKey(id);
    } catch (IOException | KeyServiceException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public byte[] write(Base message) throws IOException {
    if (message instanceof RambleBulkMessage) {
      SignedPayload p = new SignedPayload();
      p.setData(((RambleBulkMessage) message).getBulkMessage().toByteArray());
      p.setSignature(sign(p.getData()));
      return objectMapper.writeValueAsBytes(p);
    } else {
      return objectMapper.writeValueAsBytes(message);
    }
  }

  @Override
  public Base read(byte[] buf) throws IOException {
    Base activeGossipMessage = objectMapper.readValue(buf, Base.class);
    if (activeGossipMessage instanceof SignedPayload) {
      SignedPayload s = (SignedPayload) activeGossipMessage;
      if (verify(s, this.id)) {
        RambleBulkMessage rambleBulkMessage = new RambleBulkMessage();
        for (RambleMessage.Message message : RambleMessage.BulkMessage.parseFrom(s.getData()).getMessageList()) {
          rambleBulkMessage.addMessage(new org.apache.gossip.model.RambleMessage(message));
        }
        return rambleBulkMessage;
      }
      LOG.warn("Rejecting signed message " + s + " because digital signature verification failed");
      return null;
    } else {
      return activeGossipMessage;
    }
  }

  private boolean verify(SignedPayload signedPayload, String id) {
    Signature dsa;
    try {
      dsa = Signature.getInstance("SHA1withRSA");
      dsa.initVerify(this.keyStoreService.getPublicKey(id));
      dsa.update(signedPayload.getData());
      return dsa.verify(signedPayload.getSignature());
    } catch (NoSuchAlgorithmException | SignatureException | KeyServiceException | IOException | InvalidKeyException e) {
      throw new RuntimeException(e);
    }
  }

  public static ObjectMapper buildObjectMapper(GossipSettings settings) {
    ObjectMapper om = new ObjectMapper();
    om.enableDefaultTyping();
    // todo: should be specified in the configuration.
    om.registerModule(new CrdtModule());
    om.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, false);
    return om;
  }

  private byte[] sign(byte[] data) {
    Signature dsa;
    try {
      dsa = Signature.getInstance("SHA1withRSA");
      dsa.initSign(this.privateKey);
      dsa.update(data);
      return dsa.sign();
    } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
      throw new RuntimeException(e);
    }
  }
}
