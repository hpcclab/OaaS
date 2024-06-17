package org.hpcclab.oaas.crm.condition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.qos.QosConstraint;
import org.hpcclab.oaas.model.qos.QosRequirement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hpcclab.oaas.crm.condition.ConditionOperation.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionProcessorTest {
  ConditionProcessor processor;
  ObjectMapper mapper = new ObjectMapper();
  ObjectMapper yamlMapper = new YAMLMapper();

  @BeforeEach
  void beforeEach() {
    processor = new ConditionProcessor(mapper);
  }

  @Test
  void test() {
    OClass cls = new OClass();
    Condition con = Condition.builder().build();
    boolean res = processor.matches(con, cls);
    assert res;

    con = Condition.builder()
      .path("$.qos.throughput")
      .op(EQ)
      .val("100")
      .build();
    res = processor.matches(con, cls);
    assert !res;

    con = Condition.builder()
      .path("$.qos.throughput")
      .op(LT)
      .val("100")
      .build();
    res = processor.matches(con, cls);
    assert !res;

    con = Condition.builder()
      .path("$.qos.asdasdda")
      .op(IS_NULL)
      .build();
    res = processor.matches(con, cls);
    assert res;

    con = Condition.builder()
      .path("$.qosadda")
      .op(NOT_NULL)
      .build();
    res = processor.matches(con, cls);
    assert !res;
  }

  @Test
  void test2() {
    var cls = OClass.builder().qos(QosRequirement.builder().throughput(100).build())
      .build();
    var con = Condition.builder()
      .path("$.qos.throughput")
      .op(EQ)
      .val("100")
      .build();
    var res = processor.matches(con, cls);
    assertTrue(res);

    con = Condition.builder()
      .path("$.qos.throughput")
      .op(NOT_NULL)
      .build();
    res = processor.matches(con, cls);

    assertTrue(res);
    con = Condition.builder()
      .path("$.qos.throughput")
      .op(GTE)
      .val("100")
      .build();
    res = processor.matches(con, cls);
    assertTrue(res);
  }

  @Test
  void test3() throws Exception {
    var cls = OClass.builder()
      .qos(QosRequirement.builder().throughput(100)
        .availability(0.99f)
        .build()
      )
      .constraint(QosConstraint.builder()
        .persistent(false)
        .build()
      )
      .build();

    // language=yaml
    var res = check(cls, """
      path: $.qos.throughput
      op: EQ
      val: 100
      """);
    assert res;

    // language=yaml
    res = check(cls, """
      path: $.qos.throughput
      op: NEQ
      val: '100'
      """);
    assert !res;

    // language=yaml
    res = check(cls, """
      all:
       - path: $.qos.throughput
         op: EQ
         val: '100'
       - path: $.qos.availability
         op: GT
         val: '0.9'
       - path: $.constraint.persistent
         op: EQ
         val: 'false'
      """);
    assert res;
    // language=yaml
    res = check(cls, """
      all:
       - path: $.qos.throughput
         op: EQ
         val: '100'
       - path: $.qos.availability
         op: GT
         val: '0.9'
       - path: $.constraint.persistent
         op: EQ
         val: 'y'
      """);
    assert !res;
    // language=yaml
    res = check(cls, """
      any:
       - path: $.qos.throughput
         op: EQ
         val: '100'
       - path: $.qos.availability
         op: GT
         val: '0.9'
       - path: $.constraint.persistent
         op: EQ
         val: 'y'
      """);
    assert res;
  }

  boolean check(Object data, String sCon) throws JsonProcessingException {
    Condition condition = yamlMapper.readValue(sCon, Condition.class);
    return processor.matches(condition, data);
  }
}
