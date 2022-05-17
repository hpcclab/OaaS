package org.hpcclab.oaas.repository.function;


import io.vertx.core.json.Json;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.oal.ObjectAccessLangauge;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.repository.DefaultIdGenerator;
import org.hpcclab.oaas.repository.EntityRepository;
import org.hpcclab.oaas.repository.OaasObjectFactory;
import org.hpcclab.oaas.repository.impl.OaasObjectRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class FunctionRouterTest {

  boolean debug = true;

  FunctionRouter router;
  EntityRepository<String,OaasObject> objectRepo;
  MockGraphStateManager graphStateManager;
  MockTaskSubmitter taskSubmitter;

  InvocationGraphExecutor invocationGraphExecutor;

  public void setup(List<OaasObject> objects) {
    var classes = MockupData.testClasses();
    var functions = MockupData.testFunctions();
    var objectMap = Lists.mutable.ofAll(objects)
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
    List<OaasObject> objects = MockupData.testObjects();
    setup(objects);
    var oal = ObjectAccessLangauge.parse("o1:func1()");
    var ctx = router.invoke(oal)
      .await().indefinitely();

    invocationGraphExecutor.exec(ctx)
      .await().indefinitely();
    if (debug) {
      System.out.printf("TASK MAP:\n%s\n", Json.encodePrettily(taskSubmitter.map));
      System.out.printf("EDGE:\n%s\n", graphStateManager.multimap);
      System.out.printf("FUNCTION EXEC CONTEXT:\n%s\n", Json.encodePrettily(ctx));
    }
    Assertions.assertTrue(taskSubmitter.map.containsKey(ctx.getOutput().getId()));
    Assertions.assertEquals(1, taskSubmitter.map.size());
    Assertions.assertTrue(graphStateManager.multimap.isEmpty());

    var completion = new TaskCompletion()
      .setId(ctx.getOutput().getId())
      .setSuccess(true)
      .setEmbeddedRecord("{}");
  }

  @Test
  void testChainTaskInvocation() {
    List<OaasObject> objects = MockupData.testObjects();
    setup(objects);
    var oal = ObjectAccessLangauge.parse("o2:func1()");
    var ctx = router.invoke(oal)
      .await().indefinitely();

    invocationGraphExecutor.exec(ctx)
      .await().indefinitely();
    if (debug) {
      System.out.printf("TASK MAP:\n%s\n", Json.encodePrettily(taskSubmitter.map));
      System.out.printf("EDGE:\n%s\n", graphStateManager.multimap);
      System.out.printf("FUNCTION EXEC CONTEXT:\n%s\n", Json.encodePrettily(ctx));
    }
    Assertions.assertTrue(taskSubmitter.map.containsKey("o2"));
    Assertions.assertEquals(1, taskSubmitter.map.size());
    Assertions.assertFalse(graphStateManager.multimap.isEmpty());
    Assertions.assertTrue(graphStateManager.multimap.containsKey("o2"));
  }


}
