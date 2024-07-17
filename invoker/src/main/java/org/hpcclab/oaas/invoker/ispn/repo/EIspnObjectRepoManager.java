package org.hpcclab.oaas.invoker.ispn.repo;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCollectionAsync;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.invoker.ispn.GJValueMapper;
import org.hpcclab.oaas.invoker.ispn.IspnCacheCreator;
import org.hpcclab.oaas.invoker.ispn.store.ArgConnectionFactory;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.object.GOObject;
import org.hpcclab.oaas.model.object.JOObject;
import org.hpcclab.oaas.repository.ObjectRepoManager;

public class EIspnObjectRepoManager extends ObjectRepoManager {
  ClassControllerRegistry controllerRegistry;
  IspnCacheCreator cacheCreator;
  ArgConnectionFactory factory;

  public EIspnObjectRepoManager(ClassControllerRegistry controllerRegistry,
                                IspnCacheCreator cacheCreator) {
    this.cacheCreator = cacheCreator;
    this.controllerRegistry = controllerRegistry;
  }

  @Override
  public EIspnObjectRepository createRepo(OClass cls) {
    var objCache = cacheCreator.getObjectCache(cls);
    ArangoCollectionAsync connectionAsync = factory.getConnection(cls.getKey());
    ArangoCollection connection = factory.getConnectionSync(cls.getKey());
    ArgQueryService<GOObject, JOObject> queryService = new ArgQueryService<>(
      connection,
      connectionAsync,
      JOObject.class,
      new GJValueMapper()
    );
    return new EIspnObjectRepository(objCache.getAdvancedCache(), queryService);
  }

  @Override
  protected OClass load(String clsKey) {
    return controllerRegistry.getClassController(clsKey).getCls();
  }
}
