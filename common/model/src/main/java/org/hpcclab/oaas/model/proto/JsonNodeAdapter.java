package org.hpcclab.oaas.model.proto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.msgpack.jackson.dataformat.MessagePackMapper;

import java.io.IOException;

@ProtoAdapter(ObjectNode.class)
public class JsonNodeAdapter {
  ObjectMapper objectMapper = new MessagePackMapper();

  @ProtoFactory
  public ObjectNode create(byte[] bytes) throws IOException {
    return objectMapper.readValue(bytes, ObjectNode.class);
  }

  @ProtoField(1)
  public byte[] getBytes(ObjectNode node) throws JsonProcessingException {
    return objectMapper.writeValueAsBytes(node);
  }
}
