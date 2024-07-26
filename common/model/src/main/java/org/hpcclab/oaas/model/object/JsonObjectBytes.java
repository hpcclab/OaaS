package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hpcclab.oaas.model.exception.InvocationException;

import java.io.IOException;

/**
 * @author Pawissanutt
 */
public class JsonObjectBytes {
  public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
  public static final JsonObjectBytes EMPTY = new JsonObjectBytes();
  byte[] bytes;
  JsonNode objectNode;

  public JsonObjectBytes(byte[] bytes) {
    this.bytes = bytes;
  }
  public JsonObjectBytes(String json) {
    this.bytes = json.getBytes();
  }

  public JsonObjectBytes() {
  }

  @JsonCreator
  public JsonObjectBytes(JsonNode objectNode) {
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
    JsonNode jsonNode = getJsonNode();
    if (jsonNode instanceof  ObjectNode on) return on;
    return null;
  }

  @JsonValue
  public JsonNode getJsonNode() {
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

  public String getRaw() {
    if (bytes!=null && bytes.length != 0)
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
