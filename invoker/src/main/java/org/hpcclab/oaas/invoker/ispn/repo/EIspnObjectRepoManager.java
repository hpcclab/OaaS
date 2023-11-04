package org.hpcclab.oaas.invoker.ispn.repo;

import org.hpcclab.oaas.invoker.ispn.IspnCacheCreator;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.ObjectRepoManager;
import org.hpcclab.oaas.repository.ObjectRepository;

public class EIspnObjectRepoManager extends ObjectRepoManager {
  ClassRepository classRepo;
  IspnCacheCreator cacheCreator;

  public EIspnObjectRepoManager(ClassRepository classRepo, IspnCacheCreator cacheCreator) {
    this.classRepo = classRepo;
    this.cacheCreator = cacheCreator;
  }

  @Override
  public ObjectRepository createRepo(OaasClass cls) {
    var objCache = cacheCreator.getObjectCache(cls);
    return new EIspnObjectRepository(objCache.getAdvancedCache());
  }

  @Override
  protected OaasClass load(String clsKey) {
    return classRepo.get(clsKey);
  }
}
