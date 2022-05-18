package org.hpcclab.oaas.repository.function;


import io.vertx.core.json.Json;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.oal.ObjectAccessLangauge;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.repository.DefaultIdGenerator;
import org.hpcclab.oaas.repository.EntityRepository;
import org.hpcclab.oaas.repository.OaasObjectFactory;
import org.hpcclab.oaas.repository.impl.OaasObjectRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class FunctionRouterTest {

  boolean debug = true;

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
    objectRepo = cl.objectRepo;
    var idGen = new DefaultIdGenerator();
    var objectFactory = new OaasObjectFactory(idGen);
    var logical = new LogicalFunctionHandler(idGen);
    var task = new TaskFunctionHandler();
    task.objectFactory = objectFactory;
    var macro = new MacroFunctionHandler();
    macro.contextLoader = cl;
    macro.objectFactory = objectFactory;
    router = new FunctionRouter(logical, macro, task, cl);
    macro.router = router;

    graphStateManager = new MockGraphStateManager(objectRepo, objectMap);
    taskSubmitter = new MockTaskSubmitter();
    invocationGraphExecutor = new InvocationGraphExecutor(taskSubmitter,
      graphStateManager, cl);
  }

  @Test
  void testSimpleTaskInvocation() {
    var oal = ObjectAccessLangauge.parse("o1:func1()");
    var ctx = router.invoke(oal)
      .await().indefinitely();

    invocationGraphExecutor.exec(ctx)
      .await().indefinitely();
    printDebug(ctx);
    Assertions.assertTrue(taskSubmitter.map.containsKey(ctx.getOutput().getId()));
    Assertions.assertEquals(1, taskSubmitter.map.size());
    Assertions.assertTrue(graphStateManager.multimap.isEmpty());

    var completion = new TaskCompletion()
      .setId(ctx.getOutput().getId())
      .setSuccess(true)
      .setEmbeddedRecord("{}");
    invocationGraphExecutor.complete(completion)
      .await().indefinitely();
    var o = objectRepo.get(ctx.getOutput().getId());
    if (debug) {
      System.out.printf("OUT:\n%s\n", Json.encodePrettily(o));
    }
    Assertions.assertTrue(o.getStatus().getTaskStatus().isCompleted());
    Assertions.assertFalse(o.getStatus().getTaskStatus().isFailed());
  }

  void printDebug(FunctionExecContext ctx) {
    if (debug) {
      System.out.printf("TASK MAP:\n%s\n", Json.encodePrettily(taskSubmitter.map));
      System.out.printf("EDGE:\n%s\n", graphStateManager.multimap);
      System.out.printf("FUNCTION EXEC CONTEXT:\n%s\n", Json.encodePrettily(ctx));
      System.out.println("REPO OBJ:");
      for (var o: objectMap) {
        System.out.println("\t"+ o);
      }
    }
  }

  @Test
  void testChainTaskInvocation() {
    var oal = ObjectAccessLangauge.parse("o2:func1()");
    var ctx = router.invoke(oal)
      .await().indefinitely();

    invocationGraphExecutor.exec(ctx)
      .await().indefinitely();
    printDebug(ctx);
    Assertions.assertTrue(taskSubmitter.map.containsKey("o2"));
    Assertions.assertEquals(1, taskSubmitter.map.size());
    Assertions.assertFalse(graphStateManager.multimap.isEmpty());
    Assertions.assertTrue(graphStateManager.multimap.containsKey("o2"));


    var completion = new TaskCompletion()
      .setId("o2")
      .setSuccess(true)
      .setEmbeddedRecord("{}");
    invocationGraphExecutor.complete(completion)
      .await().indefinitely();
    var o2 = objectRepo.get("o2");
    if (debug) {
      System.out.printf("OBJECT o2:\n%s\n", Json.encodePrettily(o2));
    }
    Assertions.assertTrue(o2.getStatus().getTaskStatus().isCompleted());
    Assertions.assertFalse(o2.getStatus().getTaskStatus().isFailed());
    Assertions.assertTrue(taskSubmitter.map.containsKey(ctx.getOutput().getId()));
    Assertions.assertTrue(objectRepo.get(ctx.getOutput().getId()).getStatus().getTaskStatus().isSubmitted());
    Assertions.assertFalse(objectRepo.get(ctx.getOutput().getId()).getStatus().getTaskStatus().isCompleted());
    Assertions.assertFalse(objectRepo.get(ctx.getOutput().getId()).getStatus().getTaskStatus().isFailed());
  }


  @Test
  void testFailChainTaskInvocation() {
    var oal = ObjectAccessLangauge.parse("o2:func1()");
    var ctx = router.invoke(oal)
      .await().indefinitely();

    invocationGraphExecutor.exec(ctx)
      .await().indefinitely();
    printDebug(ctx);
    Assertions.assertTrue(taskSubmitter.map.containsKey("o2"));
    Assertions.assertEquals(1, taskSubmitter.map.size());
    Assertions.assertFalse(graphStateManager.multimap.isEmpty());
    Assertions.assertTrue(graphStateManager.multimap.containsKey("o2"));


    var completion = new TaskCompletion()
      .setId("o2")
      .setSuccess(false)
      .setEmbeddedRecord("{}");
    invocationGraphExecutor.complete(completion)
      .await().indefinitely();
    var o2 = objectRepo.get("o2");
    if (debug) {
      System.out.printf("OBJECT o2:\n%s\n", Json.encodePrettily(o2));
    }
    Assertions.assertTrue(o2.getStatus().getTaskStatus().isCompleted());
    Assertions.assertTrue(o2.getStatus().getTaskStatus().isFailed());
    Assertions.assertFalse(taskSubmitter.map.containsKey(ctx.getOutput().getId()));
    var outObj = objectRepo.get(ctx.getOutput().getId());
    if (debug) {
      System.out.printf("OBJECT OUT:\n%s\n", Json.encodePrettily(outObj));
    }
    Assertions.assertFalse(outObj.getStatus().getTaskStatus().isSubmitted());
    Assertions.assertFalse(outObj.getStatus().getTaskStatus().isCompleted());
    Assertions.assertTrue(outObj.getStatus().getTaskStatus().isFailed());
  }

  @Test
  void testMacroInvocation() {
    var oal = ObjectAccessLangauge.parse("o1:%s()".formatted(MockupData.MACRO_FUNC_1.getName()));
    var ctx = router.invoke(oal)
      .await().indefinitely();

    invocationGraphExecutor.exec(ctx)
      .await().indefinitely();
    printDebug(ctx);
  }
}
