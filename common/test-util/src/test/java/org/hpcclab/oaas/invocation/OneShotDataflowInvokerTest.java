package org.hpcclab.oaas.invocation;

import org.hpcclab.oaas.model.invocation.InternalInvocationNode;
import org.hpcclab.oaas.model.invocation.InvocationStatus;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.test.MockInvocationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hpcclab.oaas.test.MockupData.ATOMIC_MACRO_FUNC;
import static org.hpcclab.oaas.test.MockupData.CLS_1;

public class OneShotDataflowInvokerTest {
  MockInvocationEngine engine;

  @BeforeEach
  void setup() {
    engine = new MockInvocationEngine();
  }

  @Test
  void test() {
    var oal = ObjectAccessLanguage.parse("_%s/o1:%s".formatted(CLS_1.getKey(),ATOMIC_MACRO_FUNC.getName()));
    var req = oal.toRequest()
      .invId(engine.idGen.generate())
      .build();
    var ctx = engine.router.apply(req)
      .await().indefinitely();
    engine.dataflowInvoker.invoke(ctx)
      .await().indefinitely();
    engine.printDebug();
    var graph = ctx.getDataflowGraph();
    for (InternalInvocationNode node : graph.getAll()) {
      assertThat(node.getCtx().initNode().getStatus())
        .isEqualTo(InvocationStatus.SUCCEEDED);
//      System.out.println(node);
//      System.out.println(node.getCtx().initNode());
    }
  }
}
