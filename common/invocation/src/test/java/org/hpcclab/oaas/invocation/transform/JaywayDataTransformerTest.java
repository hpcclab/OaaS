package org.hpcclab.oaas.invocation.transform;

import org.hpcclab.oaas.model.function.Dataflows;
import org.hpcclab.oaas.model.object.JsonBytes;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author Pawissanutt
 */
class JaywayDataTransformerTest {

  @Test
  void test() {
    JaywayDataTransformer transformer = new JaywayDataTransformer(List.of(
      new Dataflows.Transformation("$.a", "aa"),
      new Dataflows.Transformation("$.b", "bb"),
      new Dataflows.Transformation("$.c[?(@.d == '111')]", "c"),
      new Dataflows.Transformation("$.c.length()", "length")
    ));
    JsonBytes out = transformer.transform(new JsonBytes("""
      {
        "a": "aa",
        "b": "bb",
        "c": [{"d":"111"},{"d":"222"},{"d":"333"}]
      }
      """.getBytes()));
    System.out.println(out);
  }

}
