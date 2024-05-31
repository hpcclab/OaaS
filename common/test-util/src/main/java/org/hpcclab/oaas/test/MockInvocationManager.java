package org.hpcclab.oaas.test;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.hpcclab.oaas.invocation.InvocationManager;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.invocation.controller.*;
import org.hpcclab.oaas.invocation.metrics.MetricFactory;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.repository.id.TsidGenerator;

/**
 * @author Pawissanutt
 */
public class MockInvocationManager {

  public static InvocationManager getInstance() {
    var registry = new BaseClassControllerRegistry();
    var repoManager =
      new MapEntityRepository.MapObjectRepoManager(Maps.mutable.empty(), registry);
    var reqHandler = new MockControllerInvocationReqHandler(
      registry,
      new RepoCtxLoader(repoManager, registry),
      new TsidGenerator());

    var builder = new RepoClassControllerBuilder(
      new MockFunctionControllerFactory(),
      new RepoStateManager(repoManager),
      new TsidGenerator(),
      new MockInvocationQueueProducer(),
      new MetricFactory.NoOpMetricFactory(),
      MockupData.fnRepo(),
      MockupData.clsRepo()
    );
    MutableMap<String, OClass> clsMap = MockupData.testClasses();
    for (OClass cls : clsMap) {
      ClassController controller = builder.build(cls).await().indefinitely();
      registry.register(controller);
    }
    return new InvocationManager(registry, builder, reqHandler);
  }
}
