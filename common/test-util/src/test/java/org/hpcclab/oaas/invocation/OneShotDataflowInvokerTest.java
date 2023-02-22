package org.hpcclab.oaas.invocation;

import org.assertj.core.api.Assertions;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.test.MockInvocationEngine;
import org.hpcclab.oaas.test.MockupData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hpcclab.oaas.test.MockupData.ATOMIC_MACRO_FUNC;

public class OneShotDataflowInvokerTest {
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
    engine.dataflowInvoker.invoke(ctx)
      .await().indefinitely();
    engine.printDebug(ctx);
  }
}
