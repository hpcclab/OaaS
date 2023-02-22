package org.hpcclab.oaas.test;

import io.vertx.core.json.Json;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
import org.hpcclab.oaas.invocation.*;
import org.hpcclab.oaas.invocation.applier.LogicalFunctionApplier;
import org.hpcclab.oaas.invocation.applier.MacroFunctionApplier;
import org.hpcclab.oaas.invocation.applier.TaskFunctionApplier;
import org.hpcclab.oaas.invocation.applier.UnifiedFunctionRouter;
import org.hpcclab.oaas.invocation.dataflow.OneShotDataflowInvoker;
import org.hpcclab.oaas.model.invocation.InvApplyingContext;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockInvocationEngine {
  private static final Logger logger = LoggerFactory.getLogger( MockInvocationEngine.class );

  public boolean debug = true;
  public final UnifiedFunctionRouter router;
  public final EntityRepository<String, OaasObject> objectRepo;
  public final MockGraphStateManager graphStateManager;
  public final MockInvocationQueueSender invocationQueueSender;
  public final MockSyncInvoker syncInvoker;
  public final InvocationExecutor invocationExecutor;
  public final MutableMap<String, OaasObject> objectMap;
  public final OneShotDataflowInvoker dataflowInvoker;
  public final CompletedStateUpdater completedStateUpdater;
  public final TaskFactory taskFactory;
  public final RepoContextLoader loader;
  public final IdGenerator idGen;

  public MockInvocationEngine() {
    var objects = MockupData.testObjects();
    var classes = MockupData.testClasses();
    var functions = MockupData.testFunctions();
    objectMap = Lists.mutable.ofAll(objects)
      .groupByUniqueKey(OaasObject::getId);
    loader = MockupData.mockContextLoader(objectMap, classes, functions);
    objectRepo = loader.getObjectRepo();
    idGen = new TsidGenerator();
    var objectFactory = new OaasObjectFactory(idGen);
    var logicalApplier = new LogicalFunctionApplier(idGen);
    var taskApplier = new TaskFunctionApplier(objectFactory);
    var macroApplier = new MacroFunctionApplier(loader, objectFactory);
    router = new UnifiedFunctionRouter(logicalApplier, macroApplier, taskApplier, loader);

    graphStateManager = new MockGraphStateManager(objectRepo);
    var contentUrlGenerator = new ContentUrlGenerator("http://localhost:8080");
    taskFactory = new TaskFactory(contentUrlGenerator, loader.getClsRepo(), new TsidGenerator());
    invocationQueueSender = new MockInvocationQueueSender(taskFactory);
    syncInvoker = new MockSyncInvoker();
    completedStateUpdater = new CompletedStateUpdater(new CompletionValidator(loader.getClsRepo(), loader.getFuncRepo()));
    invocationExecutor = new InvocationExecutor(
      invocationQueueSender,
      graphStateManager,
      loader,
      syncInvoker,
      taskFactory,
      completedStateUpdater
    );
    dataflowInvoker = new OneShotDataflowInvoker(
      syncInvoker,
      taskFactory,
      completedStateUpdater,
      graphStateManager
    );
  }

  public void printDebug(InvApplyingContext ctx) {
    if (debug && logger.isDebugEnabled()) {
      logger.debug("TASK MAP: {}", Json.encodePrettily(invocationQueueSender.multimap.toMap()));
      logger.debug("EDGE: {}", Json.encodePrettily(graphStateManager.multimap.toMap()));
      logger.debug("FUNCTION EXEC CONTEXT: {}", Json.encodePrettily(ctx));
      int i = 0;
      for (var o : objectMap) {
        logger.debug("REPO OBJ {}: {}", i, Json.encode(o));
        i++;
      }
    }
  }
}
