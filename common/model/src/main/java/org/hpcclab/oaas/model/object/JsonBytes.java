package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hpcclab.oaas.model.exception.InvocationException;

import java.io.IOException;

/**
 * @author Pawissanutt
 */
public class JsonBytes {
  public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
  public static final JsonBytes EMPTY = new JsonBytes();
  byte[] bytes;
  ObjectNode objectNode;

  public JsonBytes(byte[] bytes) {
    this.bytes = bytes;
  }

  public JsonBytes() {
  }

  @JsonCreator
  public JsonBytes(ObjectNode objectNode) {
    this.objectNode = objectNode;
  }

  public byte[] getBytes() {
    if (bytes==null && objectNode==null) {
      return EMPTY_BYTE_ARRAY;
    } else if (bytes!=null) {
      return bytes;
    }
    bytes = OObjectConverter.getInstance().convert(objectNode);
    return bytes;
  }

  public ObjectNode getNode() {
    if (bytes==null && objectNode==null) {
      return null;
    } else if (objectNode!=null) {
      return objectNode;
    }
    objectNode = OObjectConverter.getInstance().convert(bytes);
    return objectNode;
  }

  @JsonIgnore
  public ObjectNode getNodeOrEmpty() {
    var node = getNode();
    if (node != null) return node;
    return OObjectConverter.getInstance().createEmpty();
  }

  @JsonRawValue
  @JsonValue
  public String getRaw() {
    if (bytes!=null)
      return new String(getBytes());
    else if (objectNode!=null)
      return objectNode.toString();
    return "null";
  }

  public <T> T mapToObj(Class<T> clazz) {
    ObjectMapper objectMapper = OObjectConverter.getInstance().objectMapper;
    try {
      if (objectNode!=null)
        return objectMapper.treeToValue(objectNode, clazz);

      if (bytes!=null) {
        return objectMapper.readValue(bytes, clazz);
      }
    } catch (IOException e) {
      throw new InvocationException("Cannot decode JSON to " + clazz.getSimpleName(), e);
    }
    return null;
  }

  @Override
  public String toString() {
    return getRaw();
  }
}
