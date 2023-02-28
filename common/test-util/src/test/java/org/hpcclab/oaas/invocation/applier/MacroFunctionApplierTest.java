package org.hpcclab.oaas.invocation.applier;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.test.MockInvocationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hpcclab.oaas.test.MockupData.ATOMIC_MACRO_FUNC;

public class MacroFunctionApplierTest {
  MockInvocationEngine engine;

  @BeforeEach
  void setup() {
    engine = new MockInvocationEngine();
  }

  @Test
  void test() {
    var oal = ObjectAccessLanguage.parse("o1:" + ATOMIC_MACRO_FUNC.getName());
    var ctx = engine.router.apply(oal)
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
