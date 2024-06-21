package org.hpcclab.oaas.test;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.hpcclab.oaas.invocation.InvocationManager;
import org.hpcclab.oaas.invocation.controller.*;
import org.hpcclab.oaas.invocation.metrics.MetricFactory;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.repository.MapEntityRepository;
import org.hpcclab.oaas.repository.ObjectRepoManager;
import org.hpcclab.oaas.repository.id.TsidGenerator;

/**
 * @author Pawissanutt
 */
public class MockInvocationManager {

  public final MockInvocationQueueProducer invocationQueueProducer;
  public final ObjectRepoManager repoManager;
  public final InvocationManager invocationManager;

  public MockInvocationManager(MockInvocationQueueProducer invocationQueueProducer,
                               ObjectRepoManager repoManager,
                               InvocationManager invocationManager) {
    this.invocationQueueProducer = invocationQueueProducer;
    this.repoManager = repoManager;
    this.invocationManager = invocationManager;
  }

  public static MockInvocationManager getInstance() {
    var registry = new BaseClassControllerRegistry();
    var repoManager = new MapEntityRepository.MapObjectRepoManager(Maps.mutable.empty(),
      k -> registry.getClassController(k).getCls());
    var reqHandler = new CcInvocationReqHandler(
      registry,
      new RepoCtxLoader(repoManager, registry),
      new TsidGenerator(),
      Integer.MAX_VALUE
    );

    var invocationQueueProducer = new MockInvocationQueueProducer();
    var builder = new RepoClassControllerBuilder(
      new MockFunctionControllerFactory(reqHandler),
      new RepoStateManager(repoManager),
      new TsidGenerator(),
      invocationQueueProducer,
      new MetricFactory.NoOpMetricFactory(),
      MockupData.fnRepo(),
      MockupData.clsRepo()
    );
    MutableMap<String, OClass> clsMap = MockupData.testClasses();
    for (OClass cls : clsMap) {
      ClassController controller = builder.build(cls).await().indefinitely();
      registry.register(controller);
    }
    var invocationManager = new InvocationManager(
      registry,
      builder,
      reqHandler
    );
    return new MockInvocationManager(
      invocationQueueProducer,
      repoManager,
      invocationManager
    );
  }
}
