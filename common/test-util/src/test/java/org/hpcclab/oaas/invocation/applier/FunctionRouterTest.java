package org.hpcclab.oaas.invocation.applier;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.Json;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
import org.hpcclab.oaas.invocation.*;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.object.ObjectUpdate;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskIdentity;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.hpcclab.oaas.repository.EntityRepository;
import org.hpcclab.oaas.repository.OaasObjectFactory;
import org.hpcclab.oaas.repository.TsidGenerator;
import org.hpcclab.oaas.repository.UuidGenerator;
import org.hpcclab.oaas.test.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FunctionRouterTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(FunctionRouterTest.class);

  boolean debug = true;
  ObjectMapper objectMapper = new ObjectMapper();

  UnifiedFunctionRouter router;
  EntityRepository<String, OaasObject> objectRepo;
  MockGraphStateManager graphStateManager;
  MockInvocationQueueSender invocationQueueSender;
  MockSyncInvoker syncInvoker;

  InvocationExecutor invocationExecutor;
  MutableMap<String, OaasObject> objectMap;

  @BeforeEach
  public void setup() {
    var objects = MockupData.testObjects();
    var classes = MockupData.testClasses();
    var functions = MockupData.testFunctions();
    objectMap = Lists.mutable.ofAll(objects)
      .groupByUniqueKey(OaasObject::getId);
    var loader = TestUtil.mockContextLoader(objectMap, classes, functions);
    objectRepo = loader.getObjectRepo();
    var idGen = new UuidGenerator();
    var objectFactory = new OaasObjectFactory(idGen);
    var logicalApplier = new LogicalFunctionApplier(idGen);
    var taskApplier = new TaskFunctionApplier(objectFactory);
    var macroApplier = new MacroFunctionApplier();
    macroApplier.contextLoader = loader;
    macroApplier.objectFactory = objectFactory;
    router = new UnifiedFunctionRouter(logicalApplier, macroApplier, taskApplier, loader);
    macroApplier.router = router;

    graphStateManager = new MockGraphStateManager(objectRepo, objectMap);
    var contentUrlGenerator = new ContentUrlGenerator("http://localhost:8080");
    var taskFactory = new TaskFactory(contentUrlGenerator, loader.getClsRepo(), new TsidGenerator());
    invocationQueueSender = new MockInvocationQueueSender(taskFactory);
    syncInvoker = new MockSyncInvoker();
    invocationExecutor = new InvocationExecutor(
      invocationQueueSender,
      graphStateManager,
      loader,
      syncInvoker,
      taskFactory,
      new CompletionValidator(loader.getClsRepo(), loader.getFuncRepo())
    );
  }

  void printDebug(FunctionExecContext ctx) {
    if (debug) {
      LOGGER.debug("TASK MAP: {}", Json.encodePrettily(invocationQueueSender.multimap.toMap()));
      LOGGER.debug("EDGE: {}", Json.encodePrettily(graphStateManager.multimap.toMap()));
      LOGGER.debug("FUNCTION EXEC CONTEXT: {}", Json.encodePrettily(ctx));
      int i = 0;
      for (var o : objectMap) {
        LOGGER.debug("REPO OBJ {}: {}", i, Json.encode(o));
        i++;
      }
    }
  }

  @Test
  void testSimpleTaskInvocation() {
    var oal = ObjectAccessLanguage.parse("o1:f1()(aa=bb)");
    var partKey = "o1";
    var ctx = router.apply(oal)
      .await().indefinitely();

    invocationExecutor.asyncSubmit(ctx)
      .await().indefinitely();
    printDebug(ctx);
//    var taskId = new TaskIdentity(ctx);
//    var encTaskId = taskId.encode();
    assertTrue(invocationQueueSender.multimap.containsKey(partKey));
    assertEquals(1, invocationQueueSender.multimap.size());
    var request = invocationQueueSender.multimap.get(partKey).getAny();
    assertNotNull(request);
    assertTrue(graphStateManager.multimap.isEmpty());
    assertEquals("bb", request.args().get("aa"));

    syncInvoker.setMapper(detail -> new TaskCompletion()
      .setId(TaskIdentity.decode(detail.getId()))
      .setSuccess(true)
      .setOutput(new ObjectUpdate(objectMapper.createObjectNode()))
      .setMain(new ObjectUpdate().setUpdatedKeys(Set.of("k1")))
      .setCptTs(System.currentTimeMillis()));
    invocationExecutor.asyncExec(ctx)
      .await().indefinitely();
//    var completion = new TaskCompletion()
//      .setIdFromTask(task)
//      .setSuccess(true)
//      .setOutput(new ObjectUpdate( objectMapper.createObjectNode()))
//      .setMain(new ObjectUpdate().setUpdatedKeys(Set.of("k1")))
//      .setCptTs(System.currentTimeMillis());
//    invocationExecutor.complete(task, completion)
//      .await().indefinitely();
    var loadedObj = objectRepo.get(request.outId());
    LOGGER.debug("OBJECT OUT: {}", Json.encodePrettily(loadedObj));
    assertNotNull(loadedObj);
    assertNotNull(loadedObj.getStatus());
    assertTrue(loadedObj.getStatus().getCptTs() > 0);
    assertEquals(TaskStatus.SUCCEEDED,
      loadedObj.getStatus().getTaskStatus());
    assertTrue(loadedObj.getStatus().getTaskStatus().isCompleted());
    assertFalse(loadedObj.getStatus().getTaskStatus().isFailed());

    loadedObj = objectRepo.get(ctx.getMain().getId());
    LOGGER.debug("OBJECT MAIN: {}", Json.encodePrettily(loadedObj));
    assertNotEquals("kkkk", loadedObj.getState().getVerIds().get("k1"));
  }

  @Test
  void testNoOutputTaskInvocation() {
    var oal = ObjectAccessLanguage.parse("o1:func2()");
    var partKey = "o1";
    var ctx = router.apply(oal)
      .await().indefinitely();

    invocationExecutor.asyncSubmit(ctx)
      .await().indefinitely();
    printDebug(ctx);
    assertTrue(invocationQueueSender.multimap.containsKey(partKey));
    assertEquals(1, invocationQueueSender.multimap.size());
    var request = invocationQueueSender.multimap.get(partKey).getAny();
    assertNotNull(request);
    assertTrue(graphStateManager.multimap.isEmpty());

    syncInvoker.setMapper(detail -> new TaskCompletion()
      .setId(TaskIdentity.decode(detail.getId()))
      .setSuccess(true)
      .setMain(new ObjectUpdate(objectMapper.createObjectNode()
        .put("aaa", "bbb")))
      .setCptTs(System.currentTimeMillis()));
    invocationExecutor.asyncExec(ctx)
      .await().indefinitely();

    var mainObj = objectRepo.get(request.target());
    System.out.printf("OBJECT MAIN: %s%n", Json.encodePrettily(mainObj));
    Assertions.assertEquals("bbb", mainObj.getData().get("aaa").asText());
  }


  @Test
  void testChainTaskInvocation() {
    var oal = ObjectAccessLanguage.parse("o2:f1()()");
    var ctx = router.apply(oal)
      .await().indefinitely();

    invocationExecutor.asyncSubmit(ctx)
      .await().indefinitely();
    printDebug(ctx);
    assertEquals(1, invocationQueueSender.multimap.size());
    var request = invocationQueueSender.multimap
      .valuesView()
      .select(r -> r.target().equals("o1"))
      .getAny();
    assertNotNull(request);
    Assertions.assertFalse(graphStateManager.multimap.isEmpty());
    assertTrue(graphStateManager.multimap.containsKey("o2"));

    syncInvoker.setMapper(detail -> new TaskCompletion()
      .setId(TaskIdentity.decode(detail.getId()))
      .setSuccess(true)
      .setOutput(new ObjectUpdate(objectMapper.createObjectNode()))
      .setCptTs(System.currentTimeMillis()));
    ctx = router.apply(request)
      .await().indefinitely();
    invocationExecutor.asyncExec(ctx)
      .await().indefinitely();

    var o2 = objectRepo.get("o2");
    LOGGER.debug("OBJECT o2: {}", Json.encodePrettily(o2));
    assertTrue(o2.getStatus().getTaskStatus().isCompleted());
    assertFalse(o2.getStatus().getTaskStatus().isFailed());
    request = invocationQueueSender.multimap
      .valuesView()
      .select(r -> r.target().equals("o2"))
      .getAny();
    assertNotNull(request);

    var o3 = objectRepo.get(request.outId());
    assertNotNull(o3);
    assertTrue(o3.getStatus().getTaskStatus().isSubmitted());
    assertFalse(o3.getStatus().getTaskStatus().isCompleted());
    assertFalse(o3.getStatus().getTaskStatus().isFailed());
  }

  @Test
  void testFailChainTaskInvocation() {
    var oal = ObjectAccessLanguage.parse("o2:f1()");
    var ctx = router.apply(oal)
      .await().indefinitely();

    invocationExecutor.asyncSubmit(ctx)
      .await().indefinitely();
    printDebug(ctx);
    assertEquals(1, invocationQueueSender.multimap.size());
    var request = invocationQueueSender.multimap
      .valuesView()
      .select(t -> t.target().equals("o1"))
      .getAny();
    assertNotNull(request);
    Assertions.assertFalse(graphStateManager.multimap.isEmpty());
    assertTrue(graphStateManager.multimap.containsKey("o2"));


    syncInvoker.setMapper(detail -> new TaskCompletion()
      .setId(TaskIdentity.decode(detail.getId()))
      .setSuccess(false)
      .setOutput(new ObjectUpdate(objectMapper.createObjectNode())));
    var tmpCtx = router.apply(request)
      .await().indefinitely();
    invocationExecutor.asyncExec(tmpCtx)
      .await().indefinitely();


    var o2 = objectRepo.get("o2");
    LOGGER.debug("OBJECT o2: {}", Json.encodePrettily(o2));
    assertTrue(o2.getStatus().getTaskStatus().isCompleted());
    assertTrue(o2.getStatus().getTaskStatus().isFailed());
    Assertions.assertFalse(invocationQueueSender.multimap.containsKey("o2"));
    var outObj = objectRepo.get(ctx.getOutput().getId());

    LOGGER.debug("OBJECT OUT: {}", Json.encodePrettily(outObj));

    Assertions.assertFalse(outObj.getStatus().getTaskStatus().isSubmitted());
    Assertions.assertFalse(outObj.getStatus().getTaskStatus().isCompleted());
    assertTrue(outObj.getStatus().getTaskStatus().isFailed());
  }

  @Test
  void testMacroInvocation() {
    var oal = ObjectAccessLanguage.parse("o1:%s()(arg1=ttt)"
      .formatted(MockupData.MACRO_FUNC_1.getName()));
    var ctx = router.apply(oal)
      .await().indefinitely();

    invocationExecutor.asyncSubmit(ctx)
      .await().indefinitely();
    printDebug(ctx);
    assertFalse(ctx.getOutput().getStatus().getTaskStatus().isSubmitted());
    assertTrue(ctx.getSubOutputs().get(0).getStatus().getTaskStatus().isSubmitted());
    assertTrue(ctx.getSubOutputs().get(0).getOrigin().getArgs().containsKey("key1"));
  }

  @Test
  void testMacroGeneration() {
    var request = InvocationRequest.builder()
      .target("o1")
      .fbName(MockupData.MACRO_FUNC_1.getName())
      .fbName(MockupData.MACRO_FUNC_1.getName())
      .outId("m2")
      .macroIds(Map.of(
        "tmp1", "m1",
        "tmp2", "m2"
      ))
      .build();
    var ctx = router.apply(request)
      .await().indefinitely();
//    printDebug(ctx);
    assertEquals(2, ctx.getSubContexts().size());
    assertTrue(ctx.getSubOutputs().stream()
      .anyMatch(o -> o.getId().equals("m1"))
    );
    assertTrue(ctx.getSubOutputs().stream()
      .anyMatch(o -> o.getId().equals("m2"))
    );
    assertEquals("m2", ctx.getOutput().getId());
    assertEquals("1", ctx.getSubContexts().get(0).getArgs().get("STEP"));
    assertEquals("2", ctx.getSubContexts().get(1).getArgs().get("STEP"));

    invocationExecutor.asyncSubmit(ctx)
      .await().indefinitely();
//    printDebug(ctx);
    var req1 = invocationQueueSender.multimap.get("o1").getAny();
    assertEquals("1", req1.args().get("STEP"));
    var step1Ctx = router.apply(req1)
      .await().indefinitely();
    invocationExecutor.asyncExec(step1Ctx)
      .await().indefinitely();
    var req2 = invocationQueueSender.multimap.get("m1").getAny();
    assertEquals("2", req2.args().get("STEP"));
    assertEquals("f3", req2.fbName());
    printDebug(ctx);
  }
}
