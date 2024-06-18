package org.hpcclab.oaas.invocation.controller;

import org.hpcclab.oaas.invocation.InvocationManager;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.repository.ObjectRepoManager;
import org.hpcclab.oaas.test.MockInvocationManager;
import org.hpcclab.oaas.test.MockInvocationQueueProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.hpcclab.oaas.test.MockupData.*;


class MacroFnControllerTest {
  InvocationManager manager;
  InvocationReqHandler reqHandler;
  ClassControllerRegistry registry;
  MockInvocationQueueProducer producer;
  ObjectRepoManager repoManager;

  @BeforeEach
  void beforeEach() {
    MockInvocationManager instance = MockInvocationManager.getInstance();
    manager = instance.invocationManager;
    repoManager = instance.repoManager;
    producer = instance.invocationQueueProducer;
    reqHandler = manager.getReqHandler();
    registry = manager.getRegistry();
  }

  @Test
  void testExecMacro() {
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
    var id = resp.output().getKey();
    assertThat(id).isNotNull();

    request = request.toBuilder()
      .main(id)
      .fb(MACRO_FUNC_1.getName())
      .build();
    resp = reqHandler.invoke(request)
      .await().indefinitely();
    assertThat(resp.body().getNode().get("step1").asInt())
      .isEqualTo(1);
    assertThat(resp.body().getNode().get("step2").asInt())
      .isEqualTo(2);
    assertThat(resp.body().getNode().get("step3").asInt())
      .isEqualTo(3);
    System.out.println(resp);
  }

  @Test
  void testExecMacro2() {
    assertThat(registry.getClassController(CLS_1_KEY))
      .isNotNull();
    System.out.println(registry.printStructure());
    InvocationRequest request = InvocationRequest.builder()
      .cls(CLS_1.getKey())
      .fb("new")
      .args(DSMap.of("ADD1", "1","ADD2", "2","ADD3", "3"))
      .build();
    InvocationResponse resp = reqHandler.invoke(request).await().indefinitely();
    assertThat(resp.output())
      .isNotNull();
    var id = resp.output().getKey();
    assertThat(id).isNotNull();

    request = request.toBuilder()
      .main(id)
      .fb(MACRO_FUNC_2.getName())
      .build();
    resp = reqHandler.invoke(request)
      .await().indefinitely();
    System.out.println(resp);
    assertThat(resp.output().getData().getNode().get("n").asInt())
      .isEqualTo(6);
  }
}
