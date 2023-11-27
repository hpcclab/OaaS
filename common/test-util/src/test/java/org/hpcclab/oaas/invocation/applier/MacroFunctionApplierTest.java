package org.hpcclab.oaas.invocation.applier;

import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.test.MockInvocationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hpcclab.oaas.test.MockupData.ATOMIC_MACRO_FUNC;
import static org.hpcclab.oaas.test.MockupData.CLS_1;

public class MacroFunctionApplierTest {
  MockInvocationEngine engine;

  @BeforeEach
  void setup() {
    engine = new MockInvocationEngine();
  }

  @Test
  void test() {
    var oal = ObjectAccessLanguage.parse("_%s~o1:%s".formatted(CLS_1.getKey(),ATOMIC_MACRO_FUNC.getName()));
    var req = oal.toRequest()
      .invId(engine.idGen.generate())
      .build();
    var ctx = engine.router.apply(req)
      .await().indefinitely();
    var graph = ctx.getDataflowGraph();
    assertThat(graph.getEntries())
      .size().isEqualTo(2);
    assertThat(graph.getAll())
      .size().isEqualTo(3);
    var node1 = graph.getEntries().get(0);
    var node2 = graph.getEntries().get(1);
    assertThat(node1.getNext())
      .size().isEqualTo(1);
    assertThat(node2.getNext())
      .size().isEqualTo(1);
    var node3 = node1.getNext().get(0);
    assertThat(node3.getNext())
      .size().isZero();
    assertThat(node3.getCtx().getInputs())
      .size().isEqualTo(1);
  }
}
