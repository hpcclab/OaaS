package org.hpcclab.oaas.invocation.controller;

import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.invocation.InvocationManager;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.model.invocation.InvocationChain;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.repository.ObjectRepoManager;
import org.hpcclab.oaas.test.MockInvocationManager;
import org.hpcclab.oaas.test.MockInvocationQueueProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.hpcclab.oaas.test.MockupData.*;


class ChainFnControllerTest {
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
  void testGenerateFlow() {
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
      .fb(CHAIN_FUNC_1.getName())
      .build();
    resp = reqHandler.invoke(request)
      .await().indefinitely();
    System.out.println(resp);
    var invocationRequests = producer.multimap.get(request.main());
    assertThat(invocationRequests)
      .size().isEqualTo(1);
    InvocationRequest step1 = invocationRequests.getOnly();
    assertThat(step1.chains())
      .size().isEqualTo(1);
    var step2 = testProcessFlow(step1).getOnly();
    assertThat(step2.chains())
      .size().isEqualTo(1);
    var step3 = testProcessFlow(step2).getOnly();
    assertThat(step3.chains())
      .size().isZero();

  }

  MutableCollection<InvocationRequest> testProcessFlow(InvocationRequest request) {
    producer.multimap.clear();
    System.out.println(request);
    InvocationResponse resp = reqHandler.invoke(request).await().indefinitely();
    System.out.println(resp);
    if (resp.output()!=null) {
      var out = repoManager.getOrCreate(CLS_1_KEY)
        .get(resp.output().getKey());
      assertThat(out)
        .isNotNull();
    }
    List<InvocationChain> chains = request.chains();
    if (chains.isEmpty())
      return Lists.mutable.empty();
    String main = chains.getFirst().main();
    MutableCollection<InvocationRequest> requests = producer.multimap.get(main);
    System.out.println(requests);
    return requests;
  }
}
