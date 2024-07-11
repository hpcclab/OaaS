package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    JsonBytes jb = new JsonBytes(jsonString.getBytes());
    String s = mapper.writeValueAsString(jb);
    System.out.println(s);
    JsonBytes jb2 = mapper.readValue(s, JsonBytes.class);
    assertEquals("aaaa", jb2.objectNode.get("test").textValue());
  }
}
