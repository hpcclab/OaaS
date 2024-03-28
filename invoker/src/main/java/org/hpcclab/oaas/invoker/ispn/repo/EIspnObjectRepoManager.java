package org.hpcclab.oaas.invoker.ispn.repo;

import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.invoker.ispn.IspnCacheCreator;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.repository.ObjectRepoManager;
import org.hpcclab.oaas.repository.ObjectRepository;

public class EIspnObjectRepoManager extends ObjectRepoManager {
  ClassControllerRegistry controllerRegistry;
  IspnCacheCreator cacheCreator;

  public EIspnObjectRepoManager(ClassControllerRegistry controllerRegistry,
                                IspnCacheCreator cacheCreator) {
    this.cacheCreator = cacheCreator;
    this.controllerRegistry = controllerRegistry;
  }

  @Override
  public ObjectRepository createRepo(OClass cls) {
    var objCache = cacheCreator.getObjectCache(cls);
    return new EIspnObjectRepository(objCache.getAdvancedCache());
  }

  @Override
  protected OClass load(String clsKey) {
    return controllerRegistry.getClassController(clsKey).getCls();
  }
}
