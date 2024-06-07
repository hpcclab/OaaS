package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hpcclab.oaas.model.exception.InvocationException;

import java.io.IOException;

/**
 * @author Pawissanutt
 */
public class OObjectConverter {
  final ObjectMapper objectMapper;

  public OObjectConverter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public JOObject convert(POObject obj) {
    if (obj == null) return null;
    return new JOObject(obj.meta, convert(obj.getData()));
  }

  public ObjectNode convert(byte[] bytes) {
    if (bytes == null || bytes.length == 0) return null;
    try {
      return  objectMapper.readValue(bytes, ObjectNode.class);
    } catch (IOException e) {
      throw new InvocationException("Json parsing error",e);
    }
  }

  public POObject convert(JOObject obj) {
    if (obj == null) return null;
    return new POObject(obj.meta, convert(obj.data));
  }


  public byte[] convert(ObjectNode objectNode) {
    if (objectNode == null) return new byte[0];
    try {
      return objectMapper.writeValueAsBytes(objectNode);
    } catch (JsonProcessingException e) {
      throw new InvocationException("Json writing error",e);
    }
  }


  private static final OObjectConverter INSTANCE = new OObjectConverter(new ObjectMapper());
  public static OObjectConverter getInstance() {
    return INSTANCE;
  }
}
