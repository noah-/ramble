package ramble.gossip.apache;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.gossip.GossipSettings;
import org.apache.gossip.crdt.CrdtModule;
import org.apache.gossip.model.Base;
import org.apache.gossip.model.SignedPayload;
import org.apache.gossip.protocol.ProtocolManager;
import org.apache.log4j.Logger;
import ramble.crypto.KeyService;
import ramble.crypto.KeyServiceFactory;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;


public class RambleProtocolManager implements ProtocolManager {

  // For now just a copy of the JacksonProtocolManager
  // TODO: Implement support for signed payloads
  // TODO: Implement Efficient Set Reconcilliation without Prior Context

  private static final Logger LOG = Logger.getLogger(RambleProtocolManager.class);

  private final ObjectMapper objectMapper;
  private final KeyService keyService;
  private final PrivateKey privKey;
  private final MetricRegistry metricRegistry;

  /** required for reflection to work! */
  public RambleProtocolManager(GossipSettings settings, String id, MetricRegistry registry) {
    // set up object mapper.
    this.objectMapper = buildObjectMapper(settings);
    this.keyService = KeyServiceFactory.getKeyService(1024, "/tmp/ramble-keys-" + id);
    this.keyService.init();
    this.metricRegistry = registry;
    PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(this.keyService.getPrivateKey());
    KeyFactory keyFactory;
    try {
      keyFactory = KeyFactory.getInstance("DSA");
      privKey = keyFactory.generatePrivate(privKeySpec);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new RuntimeException("Failed to setup digital signatures", e);
    }
  }

  @Override
  public byte[] write(Base message) throws IOException {
    byte[] json_bytes;
      SignedPayload p = new SignedPayload();
      p.setData(objectMapper.writeValueAsString(message).getBytes());
      p.setSignature(sign(p.getData()));
      json_bytes = objectMapper.writeValueAsBytes(p);
    return json_bytes;
  }

  @Override
  public Base read(byte[] buf) throws IOException {
    Base activeGossipMessage = objectMapper.readValue(buf, Base.class);
    if (activeGossipMessage instanceof SignedPayload){
      SignedPayload s = (SignedPayload) activeGossipMessage;
      return objectMapper.readValue(s.getData(), Base.class);
    } else {
      return activeGossipMessage;
    }
  }

  private static ObjectMapper buildObjectMapper(GossipSettings settings) {
    ObjectMapper om = new ObjectMapper();
    om.enableDefaultTyping();
    // todo: should be specified in the configuration.
    om.registerModule(new CrdtModule());
    om.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, false);
    return om;
  }

  private byte[] sign(byte [] bytes){
    Signature dsa;
    try {
      dsa = Signature.getInstance("SHA1withDSA", "SUN");
      dsa.initSign(this.privKey);
      dsa.update(bytes);
      return dsa.sign();
    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException | SignatureException e) {
      throw new RuntimeException(e);
    }
  }
}
