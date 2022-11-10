package org.hpcclab.oaas.invocation.function;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.Json;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
import org.hpcclab.oaas.invocation.*;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.oal.ObjectAccessLangauge;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.hpcclab.oaas.repository.DefaultIdGenerator;
import org.hpcclab.oaas.repository.EntityRepository;
import org.hpcclab.oaas.repository.OaasObjectFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

class FunctionRouterTest {
  private static final Logger LOGGER = LoggerFactory.getLogger( FunctionRouterTest.class );

  boolean debug = true;
  ObjectMapper objectMapper = new ObjectMapper();

  FunctionRouter router;
  EntityRepository<String,OaasObject> objectRepo;
  MockGraphStateManager graphStateManager;
  MockTaskSubmitter taskSubmitter;

  InvocationGraphExecutor invocationGraphExecutor;
  MutableMap<String, OaasObject> objectMap;

  @BeforeEach
  public void setup() {

    var objects = MockupData.testObjects();
    var classes = MockupData.testClasses();
    var functions = MockupData.testFunctions();
    objectMap = Lists.mutable.ofAll(objects)
      .groupByUniqueKey(OaasObject::getId);
    var cl = TestUtil.mockContextLoader(objectMap, classes, functions);
    objectRepo = cl.getObjectRepo();
    var idGen = new DefaultIdGenerator();
    var objectFactory = new OaasObjectFactory(idGen);
    var logical = new LogicalFunctionHandler(idGen);
    var task = new TaskFunctionHandler(objectFactory);
    var macro = new MacroFunctionHandler();
    macro.contextLoader = cl;
    macro.objectFactory = objectFactory;
    router = new FunctionRouter(logical, macro, task, cl);
    macro.router = router;

    graphStateManager = new MockGraphStateManager(objectRepo, objectMap);
    taskSubmitter = new MockTaskSubmitter();
    invocationGraphExecutor = new InvocationGraphExecutor(taskSubmitter,
      graphStateManager, cl, new MockSyncInvoker());
  }

  @Test
  void testSimpleTaskInvocation() {
    var oal = ObjectAccessLangauge.parse("o1:func1()");
    var ctx = router.apply(oal)
      .await().indefinitely();

    invocationGraphExecutor.exec(ctx)
      .await().indefinitely();
    printDebug(ctx);
    assertTrue(taskSubmitter.map.containsKey(ctx.getOutput().getId()));
    assertEquals(1, taskSubmitter.map.size());
    assertTrue(graphStateManager.multimap.isEmpty());
    var loadedObj = objectRepo.get(ctx.getOutput().getId());
    assertNotNull(loadedObj);
    assertNotNull(loadedObj.getStatus());
    assertTrue(loadedObj.getStatus().getQueTs() > 0);
    assertEquals(TaskStatus.DOING,loadedObj.getStatus().getTaskStatus());

    var completion = new TaskCompletion()
      .setId(ctx.getOutput().getId())
      .setSuccess(true)
      .setEmbeddedRecord(objectMapper.createObjectNode())
      .setCmpTs(System.currentTimeMillis());
    invocationGraphExecutor.complete(completion)
      .await().indefinitely();
    var o = objectRepo.get(ctx.getOutput().getId());
    LOGGER.debug("OBJECT OUT: {}", Json.encodePrettily(o));
    assertTrue(o.getStatus().getTaskStatus().isCompleted());
    Assertions.assertFalse(o.getStatus().getTaskStatus().isFailed());
    loadedObj = objectRepo.get(ctx.getOutput().getId());
    assertNotNull(loadedObj);
    assertNotNull(loadedObj.getStatus());
    assertTrue(loadedObj.getStatus().getCptTs() > 0);
    assertEquals(TaskStatus.SUCCEEDED,
      loadedObj.getStatus().getTaskStatus());
  }

  void printDebug(FunctionExecContext ctx) {
    if (debug) {
      LOGGER.debug("TASK MAP: {}", Json.encodePrettily(taskSubmitter.map));
      LOGGER.debug("EDGE: {}", Json.encodePrettily(graphStateManager.multimap));
      LOGGER.debug("FUNCTION EXEC CONTEXT: {}", Json.encodePrettily(ctx));
      LOGGER.debug("FUNCTION EXEC CONTEXT: {}", Json.encodePrettily(ctx));
      int i = 0;
      for (var o: objectMap) {
        LOGGER.debug("REPO OBJ {}: {}", i, Json.encode(o));
        i++;
      }
    }
  }

  @Test
  void testChainTaskInvocation() {
    var oal = ObjectAccessLangauge.parse("o2:func1()()");
    var ctx = router.apply(oal)
      .await().indefinitely();

    invocationGraphExecutor.exec(ctx)
      .await().indefinitely();
    printDebug(ctx);
    assertTrue(taskSubmitter.map.containsKey("o2"));
    assertEquals(1, taskSubmitter.map.size());
    Assertions.assertFalse(graphStateManager.multimap.isEmpty());
    assertTrue(graphStateManager.multimap.containsKey("o2"));


    var completion = new TaskCompletion()
      .setId("o2")
      .setSuccess(true)
      .setEmbeddedRecord(objectMapper.createObjectNode());
    invocationGraphExecutor.complete(completion)
      .await().indefinitely();
    var o2 = objectRepo.get("o2");
    LOGGER.debug("OBJECT o2: {}", Json.encodePrettily(o2));
    assertTrue(o2.getStatus().getTaskStatus().isCompleted());
    Assertions.assertFalse(o2.getStatus().getTaskStatus().isFailed());
    assertTrue(taskSubmitter.map.containsKey(ctx.getOutput().getId()));
    assertTrue(objectRepo.get(ctx.getOutput().getId()).getStatus().getTaskStatus().isSubmitted());
    Assertions.assertFalse(objectRepo.get(ctx.getOutput().getId()).getStatus().getTaskStatus().isCompleted());
    Assertions.assertFalse(objectRepo.get(ctx.getOutput().getId()).getStatus().getTaskStatus().isFailed());
  }


  @Test
  void testFailChainTaskInvocation() {
    var oal = ObjectAccessLangauge.parse("o2:func1()");
    var ctx = router.apply(oal)
      .await().indefinitely();

    invocationGraphExecutor.exec(ctx)
      .await().indefinitely();
    printDebug(ctx);
    assertTrue(taskSubmitter.map.containsKey("o2"));
    assertEquals(1, taskSubmitter.map.size());
    Assertions.assertFalse(graphStateManager.multimap.isEmpty());
    assertTrue(graphStateManager.multimap.containsKey("o2"));


    var completion = new TaskCompletion()
      .setId("o2")
      .setSuccess(false)
      .setEmbeddedRecord(objectMapper.createObjectNode());
    invocationGraphExecutor.complete(completion)
      .await().indefinitely();
    var o2 = objectRepo.get("o2");
    LOGGER.debug("OBJECT o2: {}", Json.encodePrettily(o2));
    assertTrue(o2.getStatus().getTaskStatus().isCompleted());
    assertTrue(o2.getStatus().getTaskStatus().isFailed());
    Assertions.assertFalse(taskSubmitter.map.containsKey(ctx.getOutput().getId()));
    var outObj = objectRepo.get(ctx.getOutput().getId());

    LOGGER.debug("OBJECT OUT: {}", Json.encodePrettily(outObj));

    Assertions.assertFalse(outObj.getStatus().getTaskStatus().isSubmitted());
    Assertions.assertFalse(outObj.getStatus().getTaskStatus().isCompleted());
    assertTrue(outObj.getStatus().getTaskStatus().isFailed());
  }

  @Test
  void testMacroInvocation() {
    var oal = ObjectAccessLangauge.parse("o1:%s()(arg1=ttt)".formatted(MockupData.MACRO_FUNC_1.getName()));
    var ctx = router.apply(oal)
      .await().indefinitely();

    invocationGraphExecutor.exec(ctx)
      .await().indefinitely();
    printDebug(ctx);
    assertFalse(ctx.getOutput().getStatus().getTaskStatus().isSubmitted());
    assertTrue(ctx.getSubOutputs().get(0).getStatus().getTaskStatus().isSubmitted());
    assertTrue(ctx.getSubOutputs().get(0).getOrigin().getArgs().containsKey("key1"));
  }
}
