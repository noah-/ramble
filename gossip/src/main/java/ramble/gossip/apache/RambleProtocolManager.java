package ramble.gossip.apache;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.gossip.GossipSettings;
import org.apache.gossip.crdt.CrdtModule;
import org.apache.gossip.model.Base;
import org.apache.gossip.protocol.ProtocolManager;

import java.io.IOException;


public class RambleProtocolManager implements ProtocolManager {

  // For now just a copy of the JacksonProtocolManager
  // TODO: Implement support for signed payloads - for now just sign actual messages
  // this guarantees that no one can impersonate someone else - no one can lie saying some other use said this
  // TODO: Implement Efficient Set Reconcilliation without Prior Context
  // TODO: use protobuf instead of jackson for object serialization
  // should wrap a JacksonProtocolManager and just use protobuf for RambleMessages
  // TODO: fix this ugly reflection hack in apache-gossip

  private final ObjectMapper objectMapper;

  /**
   * Required for reflection to work!
   */
  public RambleProtocolManager(GossipSettings settings, String id, MetricRegistry registry) {
    this.objectMapper = buildObjectMapper(settings);
  }

  @Override
  public byte[] write(Base message) throws IOException {
    return this.objectMapper.writeValueAsBytes(message);
  }

  @Override
  public Base read(byte[] buf) throws IOException {
    return this.objectMapper.readValue(buf, Base.class);
  }

  public static ObjectMapper buildObjectMapper(GossipSettings settings) {
    ObjectMapper om = new ObjectMapper();
    om.enableDefaultTyping();
    // todo: should be specified in the configuration.
    om.registerModule(new CrdtModule());
    om.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, false);
    return om;
  }
}
