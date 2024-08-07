package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.io.IOException;

/**
 * @author Pawissanutt
 */
public class OObjectConverter {
  final ObjectMapper objectMapper;

  public OObjectConverter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  private static OObjectConverter instance = new OObjectConverter(new ObjectMapper());
  public static OObjectConverter useMsgPack(boolean useMsgPack) {
    instance = create(useMsgPack);
    return instance;
  }
  public static OObjectConverter getInstance() {
    return instance;
  }
  public static OObjectConverter create(boolean useMsgPack) {
    if (useMsgPack)
      return new OObjectConverter(new ObjectMapper(new MessagePackFactory()));
    else
      return new OObjectConverter(new ObjectMapper());
  }


  public JsonNode convert(byte[] bytes) {
    if (bytes == null || bytes.length == 0) return null;
    try {
      return  objectMapper.readTree(bytes);
    } catch (IOException e) {
      throw new InvocationException("Json parsing error",e);
    }
  }

  public byte[] convert(JsonNode objectNode) {
    if (objectNode == null) return new byte[0];
    try {
      return objectMapper.writeValueAsBytes(objectNode);
    } catch (JsonProcessingException e) {
      throw new InvocationException("Json writing error",e);
    }
  }


  public ObjectNode createEmpty() {
    return objectMapper.createObjectNode();
  }
}
