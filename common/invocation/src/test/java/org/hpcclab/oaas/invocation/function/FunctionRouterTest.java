package org.hpcclab.oaas.invocation.function;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.Json;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
import org.hpcclab.oaas.invocation.*;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.object.ObjectUpdate;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.hpcclab.oaas.repository.TsidGenerator;
import org.hpcclab.oaas.repository.UuidGenerator;
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

  UnifiedFunctionRouter router;
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
    var idGen = new UuidGenerator();
    var objectFactory = new OaasObjectFactory(idGen);
    var logical = new LogicalFunctionHandler(idGen);
    var task = new TaskFunctionHandler(objectFactory);
    var macro = new MacroFunctionHandler();
    macro.contextLoader = cl;
    macro.objectFactory = objectFactory;
    router = new UnifiedFunctionRouter(logical, macro, task, cl);
    macro.router = router;

    graphStateManager = new MockGraphStateManager(objectRepo, objectMap);
    var contentUrlGenerator = new ContentUrlGenerator("http://localhost:8080");
    var taskFactory = new TaskFactory(contentUrlGenerator, cl.getClsRepo(), new TsidGenerator());
    taskSubmitter = new MockTaskSubmitter(taskFactory);
    invocationGraphExecutor = new InvocationGraphExecutor(taskSubmitter,
      graphStateManager, cl, new MockSyncInvoker());
  }

  @Test
  void testSimpleTaskInvocation() {
    var oal = ObjectAccessLanguage.parse("o1:func1()");
    var ctx = router.apply(oal)
      .await().indefinitely();

    invocationGraphExecutor.exec(ctx)
      .await().indefinitely();
    printDebug(ctx);
    assertTrue(taskSubmitter.multimap.containsKey(ctx.getMain().getId()));
    assertEquals(1, taskSubmitter.multimap.size());
    var task = taskSubmitter.multimap.get(ctx.getMain().getId()).getAny();
    assertNotNull(task);
    assertTrue(graphStateManager.multimap.isEmpty());
    var loadedObj = objectRepo.get(ctx.getOutput().getId());
    assertNotNull(loadedObj);
    assertNotNull(loadedObj.getStatus());
    assertTrue(loadedObj.getStatus().getQueTs() > 0);
    assertEquals(TaskStatus.DOING,loadedObj.getStatus().getTaskStatus());

    var completion = new TaskCompletion()
      .setId(task.getId())
      .setSuccess(true)
      .setOutput(new ObjectUpdate( objectMapper.createObjectNode()))
      .setCptTs(System.currentTimeMillis());
    invocationGraphExecutor.complete(task, completion)
      .await().indefinitely();
    var o = objectRepo.get(ctx.getOutput().getId());
    System.out.printf("OBJECT OUT: %s%n", Json.encodePrettily(o));
    assertTrue(o.getStatus().getTaskStatus().isCompleted());
    Assertions.assertFalse(o.getStatus().getTaskStatus().isFailed());
    loadedObj = objectRepo.get(ctx.getOutput().getId());
    assertNotNull(loadedObj);
    assertNotNull(loadedObj.getStatus());
    assertTrue(loadedObj.getStatus().getCptTs() > 0);
    assertEquals(TaskStatus.SUCCEEDED,
      loadedObj.getStatus().getTaskStatus());
  }

  @Test
  void testNoOutputTaskInvocation() {
    var oal = ObjectAccessLanguage.parse("o1:func2()");
    var ctx = router.apply(oal)
      .await().indefinitely();

    invocationGraphExecutor.exec(ctx)
      .await().indefinitely();
    printDebug(ctx);
    assertTrue(taskSubmitter.multimap.containsKey(ctx.getMain().getId()));
    assertEquals(1, taskSubmitter.multimap.size());
    var task = taskSubmitter.multimap.get(ctx.getMain().getId()).getAny();
    assertNotNull(task);
    assertTrue(graphStateManager.multimap.isEmpty());
//    var loadedObj = objectRepo.get(ctx.getOutput().getId());
//    assertNotNull(loadedObj);
//    assertNotNull(loadedObj.getStatus());
//    assertTrue(loadedObj.getStatus().getQueTs() > 0);
//    assertEquals(TaskStatus.DOING,loadedObj.getStatus().getTaskStatus());

    var completion = new TaskCompletion()
      .setId(task.getId())
      .setSuccess(true)
      .setMain(new ObjectUpdate( objectMapper.createObjectNode()
        .put("aaa", "bbb")))
      .setCptTs(System.currentTimeMillis());
    invocationGraphExecutor.complete(task, completion)
      .await().indefinitely();
    var o = objectRepo.get(ctx.getMain().getId());
    System.out.printf("OBJECT MAIN: %s%n", Json.encodePrettily(o));
    Assertions.assertEquals("bbb", o.getData().get("aaa").asText());
  }

  void printDebug(FunctionExecContext ctx) {
    if (debug) {
      LOGGER.debug("TASK MAP: {}", Json.encodePrettily(taskSubmitter.multimap.toMap()));
      LOGGER.debug("EDGE: {}", Json.encodePrettily(graphStateManager.multimap.toMap()));
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
    var oal = ObjectAccessLanguage.parse("o2:func1()()");
    var ctx = router.apply(oal)
      .await().indefinitely();

    invocationGraphExecutor.exec(ctx)
      .await().indefinitely();
    printDebug(ctx);
    assertEquals(1, taskSubmitter.multimap.size());
    var task = taskSubmitter.multimap.get("o1").getAny();
    assertNotNull(task);
    Assertions.assertFalse(graphStateManager.multimap.isEmpty());
    assertTrue(graphStateManager.multimap.containsKey("o2"));


    var completion = new TaskCompletion()
      .setId("o1")
      .setSuccess(true)
      .setOutput(new ObjectUpdate( objectMapper.createObjectNode()));
    invocationGraphExecutor.complete(task, completion)
      .await().indefinitely();
    var o2 = objectRepo.get("o2");
    LOGGER.debug("OBJECT o2: {}", Json.encodePrettily(o2));
    assertTrue(o2.getStatus().getTaskStatus().isCompleted());
    Assertions.assertFalse(o2.getStatus().getTaskStatus().isFailed());
    assertTrue(taskSubmitter.multimap.containsKey("o2"));
    assertTrue(objectRepo.get(ctx.getOutput().getId()).getStatus().getTaskStatus().isSubmitted());
    Assertions.assertFalse(objectRepo.get(ctx.getOutput().getId()).getStatus().getTaskStatus().isCompleted());
    Assertions.assertFalse(objectRepo.get(ctx.getOutput().getId()).getStatus().getTaskStatus().isFailed());
  }



  @Test
  void testFailChainTaskInvocation() {
    var oal = ObjectAccessLanguage.parse("o2:func1()");
    var ctx = router.apply(oal)
      .await().indefinitely();

    invocationGraphExecutor.exec(ctx)
      .await().indefinitely();
    printDebug(ctx);
    assertEquals(1, taskSubmitter.multimap.size());
    var task = taskSubmitter.multimap.get("o1").getAny();
    assertNotNull(task);
    Assertions.assertFalse(graphStateManager.multimap.isEmpty());
    assertTrue(graphStateManager.multimap.containsKey("o2"));


    var completion = new TaskCompletion()
      .setId("o2")
      .setSuccess(false)
      .setOutput(new ObjectUpdate( objectMapper.createObjectNode()));
    invocationGraphExecutor.complete(task, completion)
      .await().indefinitely();
    var o2 = objectRepo.get("o2");
    LOGGER.debug("OBJECT o2: {}", Json.encodePrettily(o2));
    assertTrue(o2.getStatus().getTaskStatus().isCompleted());
    assertTrue(o2.getStatus().getTaskStatus().isFailed());
    Assertions.assertFalse(taskSubmitter.multimap.containsKey("o2"));
    var outObj = objectRepo.get(ctx.getOutput().getId());

    LOGGER.debug("OBJECT OUT: {}", Json.encodePrettily(outObj));

    Assertions.assertFalse(outObj.getStatus().getTaskStatus().isSubmitted());
    Assertions.assertFalse(outObj.getStatus().getTaskStatus().isCompleted());
    assertTrue(outObj.getStatus().getTaskStatus().isFailed());
  }

  @Test
  void testMacroInvocation() {
    var oal = ObjectAccessLanguage.parse("o1:%s()(arg1=ttt)".formatted(MockupData.MACRO_FUNC_1.getName()));
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
