package org.hpcclab.oaas.invoker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.msgpack.jackson.dataformat.MessagePackMapper;

/**
 * @author Pawissanutt
 */
public class RandomTest {
  @Test
  public void test() throws JsonProcessingException {
    ObjectMapper objectMapper = new MessagePackMapper();
    var objectNode = objectMapper.createObjectNode();
    objectNode.put("test", "aaa");
    System.out.println(objectNode);
    byte[] bytes = objectMapper.writeValueAsBytes(objectNode);
    System.out.println(bytes.length);
    System.out.println(new String(bytes));
  }
}
