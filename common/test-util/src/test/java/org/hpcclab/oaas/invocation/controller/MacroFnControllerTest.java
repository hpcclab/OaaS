package org.hpcclab.oaas.invocation.controller;

import org.hpcclab.oaas.invocation.InvocationManager;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.test.MockInvocationManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.hpcclab.oaas.test.MockupData.CLS_1;
import static org.hpcclab.oaas.test.MockupData.CLS_1_KEY;


class MacroFnControllerTest {
  InvocationManager manager;
  InvocationReqHandler reqHandler;
  ClassControllerRegistry registry;

  @BeforeEach
  void beforeEach() {
    manager = MockInvocationManager.getInstance();
    reqHandler = manager.getReqHandler();
    registry = manager.getRegistry();
  }

  @Test
  void test() {
    assertThat(registry.getClassController(CLS_1_KEY))
      .isNotNull();
    System.out.println(registry.printStructure());
    InvocationRequest request = InvocationRequest.builder()
      .cls(CLS_1.getKey())
      .fb("new")
      .build();
    InvocationResponse resp = reqHandler.invoke(request).await().indefinitely();
    assertThat(resp.output())
      .isNotNull();
    var id = resp.output().getId();
    assertThat(id).isNotNull();
    request = request.toBuilder()
      .main(id)
      .fb("")
      .build();
  }
}
