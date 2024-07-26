package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Pawissanutt
 */
class JsonBytesTest {

  ObjectMapper mapper = new ObjectMapper();

  @Test
  void test() throws JsonProcessingException {
    String jsonString = """
      {"test":"aaaa"}
      """;
    JsonBytes jb = new JsonBytes(jsonString);
    String s = mapper.writeValueAsString(jb);
    JsonBytes jb2 = mapper.readValue(s, JsonBytes.class);
    assertEquals("aaaa", jb2.objectNode.get("test").textValue());
  }

  @Test
  void testNull() throws JsonProcessingException {
    String s = "null";
    JsonBytes jb = new JsonBytes(s.getBytes());
    assertNull(jb.getNode());
    String out = mapper.writeValueAsString(jb);
    assertEquals("null", out);
    out = mapper.writeValueAsString(JsonBytes.EMPTY);
    assertEquals("null", out);
  }
}
