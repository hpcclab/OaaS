package org.hpcclab.oaas.invocation.applier;


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
import org.hpcclab.oaas.model.task.TaskIdentity;
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

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FunctionRouterTest {
  private static final Logger LOGGER = LoggerFactory.getLogger( FunctionRouterTest.class );

  boolean debug = true;
  ObjectMapper objectMapper = new ObjectMapper();

  UnifiedFunctionRouter router;
  EntityRepository<String,OaasObject> objectRepo;
  MockGraphStateManager graphStateManager;
  MockTaskSubmitter taskSubmitter;

  InvocationExecutor invocationGraphExecutor;
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
    var logical = new LogicalFunctionApplier(idGen);
    var task = new TaskFunctionApplier(objectFactory);
    var macro = new MacroFunctionApplier();
    macro.contextLoader = cl;
    macro.objectFactory = objectFactory;
    router = new UnifiedFunctionRouter(logical, macro, task, cl);
    macro.router = router;

    graphStateManager = new MockGraphStateManager(objectRepo, objectMap);
    var contentUrlGenerator = new ContentUrlGenerator("http://localhost:8080");
    var taskFactory = new TaskFactory(contentUrlGenerator, cl.getClsRepo(), new TsidGenerator());
    taskSubmitter = new MockTaskSubmitter(taskFactory);
    invocationGraphExecutor = new InvocationExecutor(taskSubmitter,
      graphStateManager, cl, new MockSyncInvoker(),
      new CompletionValidator(cl.getClsRepo(), cl.getFuncRepo()));
  }

  @Test
  void testSimpleTaskInvocation() {
    var oal = ObjectAccessLanguage.parse("o1:f1()(aa=bb)");
    var ctx = router.apply(oal)
      .await().indefinitely();

    invocationGraphExecutor.asyncSubmit(ctx)
      .await().indefinitely();
    printDebug(ctx);
    var taskId = new TaskIdentity(ctx);
    var encTaskId = taskId.encode();
    assertTrue(taskSubmitter.multimap.containsKey(encTaskId));
    assertEquals(1, taskSubmitter.multimap.size());
    var task = taskSubmitter.multimap.get(encTaskId).getAny();
    assertNotNull(task);
    assertTrue(graphStateManager.multimap.isEmpty());
    assertEquals("bb", task.getArgs().get("aa"));

    var loadedObj = objectRepo.get(taskId.oId());
    assertNotNull(loadedObj);
    assertNotNull(loadedObj.getStatus());
    assertTrue(loadedObj.getStatus().getQueTs() > 0);
    assertEquals(TaskStatus.DOING,loadedObj.getStatus().getTaskStatus());

    var completion = new TaskCompletion()
      .setIdFromTask(task)
      .setSuccess(true)
      .setOutput(new ObjectUpdate( objectMapper.createObjectNode()))
      .setMain(new ObjectUpdate().setUpdatedKeys(Set.of("k1")))
      .setCptTs(System.currentTimeMillis());
    invocationGraphExecutor.complete(task, completion)
      .await().indefinitely();
    loadedObj = objectRepo.get(taskId.oId());
    LOGGER.debug("OBJECT OUT: {}", Json.encodePrettily(loadedObj));
    assertNotNull(loadedObj);
    assertNotNull(loadedObj.getStatus());
    assertTrue(loadedObj.getStatus().getCptTs() > 0);
    assertEquals(TaskStatus.SUCCEEDED,
      loadedObj.getStatus().getTaskStatus());
    assertTrue(loadedObj.getStatus().getTaskStatus().isCompleted());
    assertFalse(loadedObj.getStatus().getTaskStatus().isFailed());

    loadedObj = objectRepo.get(taskId.mId());
    LOGGER.debug("OBJECT MAIN: {}", Json.encodePrettily(loadedObj));
    assertNotEquals("kkkk", loadedObj.getState().getVerIds().get("k1"));
  }

  @Test
  void testNoOutputTaskInvocation() {
    var oal = ObjectAccessLanguage.parse("o1:func2()");
    var ctx = router.apply(oal)
      .await().indefinitely();

    invocationGraphExecutor.asyncSubmit(ctx)
      .await().indefinitely();
    printDebug(ctx);
    var taskId = new TaskIdentity(ctx);
    var encTaskId = taskId.encode();
    assertTrue(taskSubmitter.multimap.containsKey(encTaskId));
    assertEquals(1, taskSubmitter.multimap.size());
    var task = taskSubmitter.multimap.get(encTaskId).getAny();
    assertNotNull(task);
    assertTrue(graphStateManager.multimap.isEmpty());

    var completion = new TaskCompletion()
      .setIdFromTask(task)
      .setSuccess(true)
      .setMain(new ObjectUpdate( objectMapper.createObjectNode()
        .put("aaa", "bbb")))
      .setCptTs(System.currentTimeMillis());
    invocationGraphExecutor.complete(task, completion)
      .await().indefinitely();
    var mainObj = objectRepo.get(taskId.mId());
    System.out.printf("OBJECT MAIN: %s%n", Json.encodePrettily(mainObj));
    Assertions.assertEquals("bbb", mainObj.getData().get("aaa").asText());
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
    var oal = ObjectAccessLanguage.parse("o2:f1()()");
    var ctx = router.apply(oal)
      .await().indefinitely();

    invocationGraphExecutor.asyncSubmit(ctx)
      .await().indefinitely();
    printDebug(ctx);
    var taskId = new TaskIdentity(ctx);
    var encTaskId = taskId.encode();
    assertEquals(1, taskSubmitter.multimap.size());
    var task = taskSubmitter.multimap
      .valuesView()
      .select(t -> t.getMain().getId().equals("o1"))
      .getAny();
    assertNotNull(task);
    Assertions.assertFalse(graphStateManager.multimap.isEmpty());
    assertTrue(graphStateManager.multimap.containsKey("o2"));


    var completion = new TaskCompletion()
      .setIdFromTask(task)
      .setSuccess(true)
      .setOutput(new ObjectUpdate( objectMapper.createObjectNode()));
    invocationGraphExecutor.complete(task, completion)
      .await().indefinitely();
    var o2 = objectRepo.get("o2");
    LOGGER.debug("OBJECT o2: {}", Json.encodePrettily(o2));
    assertTrue(o2.getStatus().getTaskStatus().isCompleted());
    Assertions.assertFalse(o2.getStatus().getTaskStatus().isFailed());
    assertFalse(taskSubmitter.multimap
      .valuesView()
      .select(t -> t.getMain().getId().equals("o2"))
      .isEmpty()
    );
    var o3 = objectRepo.get(ctx.getOutput().getId());
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

    invocationGraphExecutor.asyncSubmit(ctx)
      .await().indefinitely();
    printDebug(ctx);
    assertEquals(1, taskSubmitter.multimap.size());
    var task = taskSubmitter.multimap
      .valuesView()
      .select(t -> t.getMain().getId().equals("o1"))
      .getAny();
    assertNotNull(task);
    Assertions.assertFalse(graphStateManager.multimap.isEmpty());
    assertTrue(graphStateManager.multimap.containsKey("o2"));


    var completion = new TaskCompletion()
      .setIdFromTask(task)
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
    var oal = ObjectAccessLanguage.parse("o1:%s()(arg1=ttt)"
      .formatted(MockupData.MACRO_FUNC_1.getName()));
    var ctx = router.apply(oal)
      .await().indefinitely();

    invocationGraphExecutor.asyncSubmit(ctx)
      .await().indefinitely();
    printDebug(ctx);
    assertFalse(ctx.getOutput().getStatus().getTaskStatus().isSubmitted());
    assertTrue(ctx.getSubOutputs().get(0).getStatus().getTaskStatus().isSubmitted());
    assertTrue(ctx.getSubOutputs().get(0).getOrigin().getArgs().containsKey("key1"));
  }
}
