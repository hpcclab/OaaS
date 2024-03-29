package org.hpcclab.oaas.invocation;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.Json;
import org.eclipse.collections.api.map.MutableMap;
import org.hpcclab.oaas.invocation.applier.UnifiedFunctionRouter;
import org.hpcclab.oaas.model.proto.KvPair;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.object.ObjectUpdate;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskIdentity;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.hpcclab.oaas.repository.EntityRepository;
import org.hpcclab.oaas.repository.GraphStateManager;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.hpcclab.oaas.test.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class MockingInvocationTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(MockingInvocationTest.class);
  ObjectMapper objectMapper = new ObjectMapper();

  UnifiedFunctionRouter router;
  EntityRepository<String, OaasObject> objectRepo;
  GraphStateManager graphStateManager;
  MockInvocationQueueSender invocationQueueSender;
  MockOffLoader syncInvoker;

  InvocationExecutor invocationExecutor;
  MutableMap<String, OaasObject> objectMap;

  MockInvocationEngine mockEngine;
  IdGenerator idGenerator;

  @BeforeEach
  public void setup() {
    mockEngine = new MockInvocationEngine();
    router = mockEngine.router;
    objectRepo = mockEngine.objectRepo;
    graphStateManager = mockEngine.graphStateManager;
    invocationQueueSender = mockEngine.invocationQueueSender;
    syncInvoker = mockEngine.syncInvoker;
    invocationExecutor = mockEngine.invocationExecutor;
    objectMap = mockEngine.objectMap;
    idGenerator = mockEngine.idGen;
  }

  @Test
  void testSimpleTaskInvocation() {
    var oal = ObjectAccessLanguage.parse("o1:f1()(aa=bb)");
    var req = oal.toRequest()
      .invId(idGenerator.generate())
      .outId(idGenerator.generate())
      .build();

    var ctx = router.apply(req)
      .await().indefinitely();

    assertThat(req.args())
      .containsEntry("aa", "bb");

    syncInvoker.setMapper(detail -> new TaskCompletion()
      .setId(TaskIdentity.decode(detail.getId()))
      .setSuccess(true)
      .setOutput(new ObjectUpdate(objectMapper.createObjectNode()))
      .setMain(new ObjectUpdate().setUpdatedKeys(Set.of("k1")))
      .setCptTs(System.currentTimeMillis()));
    ctx = invocationExecutor.asyncExec(ctx)
      .await().indefinitely();
    var loadedObj = objectRepo.get(req.outId());
    var invNode = ctx.getNode();
    LOGGER.debug("INV NODE: {}", Json.encodePrettily(invNode));
    LOGGER.debug("OBJECT OUT: {}", Json.encodePrettily(loadedObj));
    assertNotNull(loadedObj);

    assertNotNull(invNode);
    assertNotNull(invNode.getStatus());
    assertThat(invNode.getCptTs())
      .isPositive();
    assertThat(invNode.getStatus())
      .isEqualTo(TaskStatus.SUCCEEDED);

    loadedObj = objectRepo.get(ctx.getMain().getId());
    LOGGER.debug("OBJECT MAIN: {}", Json.encodePrettily(loadedObj));
    assertThat(loadedObj.getState().getVerIds())
      .doesNotContain(new KvPair("k1", "kkkk"));
  }

  @Test
  void testNoOutputTaskInvocation() {
    var oal = ObjectAccessLanguage.parse("o1:func2()");
    var req = oal.toRequest()
      .invId(idGenerator.generate())
      .build();
    var ctx = router.apply(req)
      .await().indefinitely();

    syncInvoker.setMapper(detail -> new TaskCompletion()
      .setId(TaskIdentity.decode(detail.getId()))
      .setSuccess(true)
      .setMain(new ObjectUpdate(objectMapper.createObjectNode()
        .put("aaa", "bbb")))
      .setCptTs(System.currentTimeMillis()));
    invocationExecutor.asyncExec(ctx)
      .await().indefinitely();

    var mainObj = objectRepo.get(req.main());
    System.out.printf("OBJECT MAIN: %s%n", Json.encodePrettily(mainObj));
    Assertions.assertEquals("bbb", mainObj.getData().get("aaa").asText());
  }

  @Test
  void testMacroInvocation() {
    var oal = ObjectAccessLanguage.parse("o1:%s()(arg1=ttt)"
      .formatted(MockupData.MACRO_FUNC_1.getName()));
    var req = oal.toRequest()
      .invId(idGenerator.generate())
      .outId(idGenerator.generate())
      .build();
    var ctx = router.apply(req)
      .await().indefinitely();

    invocationExecutor.disaggregateMacro(ctx)
      .await().indefinitely();
    mockEngine.printDebug(ctx);
    var node1 = mockEngine.invRepo.get(ctx.getWorkflowMap().get("tmp1").getKey());
    var node2 = mockEngine.invRepo.get(ctx.getWorkflowMap().get("tmp2").getKey());
    assertNotNull(node1);
    assertNotNull(node2);
    assertTrue(node1.getStatus().isSubmitted());
    assertFalse(node2.getStatus().isSubmitted());
  }

  @Test
  void testMacroGeneration() {
    var request = InvocationRequest.builder()
      .invId(idGenerator.generate())
      .main("o1")
      .fb(MockupData.MACRO_FUNC_1.getName())
      .outId("m2")
      .macroIds(Map.of(
        "tmp1", "m1",
        "tmp2", "m2",
        "tmp3", "m3"
      ))
      .build();
    var ctx = router.apply(request)
      .await().indefinitely();
//    mockEngine.printDebug(ctx);
    assertEquals(3, ctx.getSubContexts().size());
    assertTrue(ctx.getSubOutputs().stream()
      .anyMatch(o -> o.getId().equals("m1"))
    );
    assertTrue(ctx.getSubOutputs().stream()
      .anyMatch(o -> o.getId().equals("m2"))
    );
    assertTrue(ctx.getSubOutputs().stream()
      .anyMatch(o -> o.getId().equals("m3"))
    );
    assertEquals("m3", ctx.getOutput().getId());
    assertEquals("1", ctx.getSubContexts().get(0).getArgs().get("STEP"));
    assertEquals("2", ctx.getSubContexts().get(1).getArgs().get("STEP"));
    assertEquals("3", ctx.getSubContexts().get(2).getArgs().get("STEP"));

    invocationExecutor.disaggregateMacro(ctx)
      .await().indefinitely();
    var req1 = invocationQueueSender.multimap.get("o1").getAny();
    assertEquals("1", req1.args().get("STEP"));
    var step1Ctx = router.apply(req1)
      .await().indefinitely();
    invocationExecutor.asyncExec(step1Ctx)
      .await().indefinitely();
    mockEngine.printDebug(ctx);
    var req2 = invocationQueueSender.multimap.get("m1").getAny();
    assertEquals("2", req2.args().get("STEP"));
    assertEquals("f3", req2.fb());
    mockEngine.printDebug(ctx);
  }
}
