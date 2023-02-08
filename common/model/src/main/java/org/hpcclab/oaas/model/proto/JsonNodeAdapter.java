package org.hpcclab.oaas.model.proto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@ProtoAdapter(ObjectNode.class)
public class JsonNodeAdapter {
  ObjectMapper objectMapper = new ObjectMapper();

    @ProtoFactory
    public ObjectNode create(String raw) throws JsonProcessingException {
        return objectMapper.readValue(raw, ObjectNode.class);
    }

    @ProtoField(1)
    public String getRaw(ObjectNode node) {
        return node.toString();
    }
}
