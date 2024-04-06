package org.hpcclab.oaas.test;

import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.invocation.controller.CcInvocationReqHandler;
import org.hpcclab.oaas.invocation.controller.CtxLoader;
import org.hpcclab.oaas.invocation.controller.RepoCtxLoader;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.hpcclab.oaas.repository.id.TsidGenerator;

public class MockControllerInvocationReqHandler extends CcInvocationReqHandler {
  public MockControllerInvocationReqHandler(ClassControllerRegistry classControllerRegistry, CtxLoader ctxLoader, IdGenerator idGenerator) {
    super(classControllerRegistry, ctxLoader, idGenerator);
  }

  public static MockControllerInvocationReqHandler mock() {
    MapEntityRepository.MapObjectRepoManager repoManager = new MapEntityRepository.MapObjectRepoManager(Maps.mutable.empty(), MockupData.testClasses());
    var classControllerRegistry = MockClassControllerRegistry.mock(repoManager);
    RepoCtxLoader repoCtxLoader = new RepoCtxLoader(repoManager, classControllerRegistry);
    return new MockControllerInvocationReqHandler(
      classControllerRegistry,
      repoCtxLoader,
      new TsidGenerator());
  }

  public ClassControllerRegistry getClassControllerRegistry() {
    return classControllerRegistry;
  }

  public CtxLoader getCtxLoader() {
    return ctxLoader;
  }

  public IdGenerator getIdGenerator() {
    return idGenerator;
  }
}
