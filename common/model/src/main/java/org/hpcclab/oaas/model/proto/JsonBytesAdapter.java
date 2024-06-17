package org.hpcclab.oaas.model.proto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hpcclab.oaas.model.object.JsonBytes;
import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.msgpack.jackson.dataformat.MessagePackMapper;

import java.io.IOException;

/**
 * @author Pawissanutt
 */
@ProtoAdapter(JsonBytes.class)
public class JsonBytesAdapter {

  @ProtoFactory
  public JsonBytes create(byte[] bytes) {
    return new JsonBytes(bytes);
  }

  @ProtoField(1)
  public byte[] getBytes(JsonBytes bytes) {
    return bytes.getBytes();
  }
}
