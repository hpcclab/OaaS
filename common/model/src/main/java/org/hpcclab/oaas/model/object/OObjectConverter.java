package org.hpcclab.oaas.model.object;

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
    try {
      if (obj.getData() == null || obj.getData().length == 0)
        return new JOObject(obj.meta, null);
      else
        return new JOObject(obj.meta, objectMapper.readValue(obj.getData(), ObjectNode.class));
    } catch (IOException e) {
      throw new InvocationException("Json parsing error",e);
    }
  }

  public POObject convert(JOObject obj) {
    if (obj == null) return null;
    try {
      if (obj.data != null)
        return new POObject(obj.meta, objectMapper.writeValueAsBytes(obj.data));
      else
        return new POObject(obj.meta, null);
    } catch (IOException e) {
      throw new InvocationException("Json parsing error",e);
    }
  }

  private static final OObjectConverter INSTANCE = new OObjectConverter(new ObjectMapper());
  public static OObjectConverter getInstance() {
    return INSTANCE;
  }
}
