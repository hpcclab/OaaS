package org.hpcclab.oaas.test;

import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.hpcclab.oaas.invocation.InvocationQueueProducer;
import org.hpcclab.oaas.invocation.controller.AbsClassControllerRegistry;
import org.hpcclab.oaas.invocation.controller.RepoStateManager;
import org.hpcclab.oaas.invocation.controller.StateManager;
import org.hpcclab.oaas.invocation.controller.fn.FunctionControllerFactory;
import org.hpcclab.oaas.invocation.metrics.MetricFactory;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.hpcclab.oaas.repository.id.TsidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

public class MockClassControllerRegistry extends AbsClassControllerRegistry {
  private static final Logger logger = LoggerFactory.getLogger( MockClassControllerRegistry.class );
  MutableMap<String, OClass> clsMap;
  MutableMap<String, OFunction> fnMap;

  public static MockClassControllerRegistry mock() {
    MutableMap<String, OClass> clsMap = MockupData.testClasses();
    MockClassControllerRegistry registry = new MockClassControllerRegistry(
      new MockFunctionControllerFactory(),
      new RepoStateManager(new MapEntityRepository.MapObjectRepoManager(Maps.mutable.empty(), clsMap)),
      new TsidGenerator(), new MockInvocationQueueProducer(),
      new MetricFactory.NoOpMetricFactory()
    );
    for (OClass cls : clsMap) {
      registry.registerOrUpdate(cls).await().indefinitely();
    }
    return registry;
  }

  protected MockClassControllerRegistry(FunctionControllerFactory functionControllerFactory, StateManager stateManager, IdGenerator idGenerator, InvocationQueueProducer invocationQueueProducer, MetricFactory metricFactory) {
    super(functionControllerFactory, stateManager, idGenerator, invocationQueueProducer, metricFactory);
    clsMap = MockupData.testClasses();
    fnMap = MockupData.testFunctions();
  }

  @Override
  protected Uni<Map<String, OClass>> listCls(Set<String> keys) {
    logger.debug("listCls {}", keys);
    MutableMap<String, OClass> selected = clsMap.select((k, v) -> keys.contains(k));
    return Uni.createFrom().item(selected);
  }

  @Override
  protected Uni<Map<String, OFunction>> listFn(Set<String> keys) {
    logger.debug("listFn {}", keys);
    return Uni.createFrom().item(fnMap.select((k, v) -> keys.contains(k)));
  }
}
