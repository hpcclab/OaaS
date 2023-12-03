package org.hpcclab.oaas.invoker.ispn.repo;

import org.hpcclab.oaas.invoker.ispn.IspnCacheCreator;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.repository.*;

public class EIspnInvRepoManager extends InvRepoManager {
  ClassRepository classRepo;
  IspnCacheCreator cacheCreator;

  public EIspnInvRepoManager(ClassRepository classRepo, IspnCacheCreator cacheCreator) {
    this.classRepo = classRepo;
    this.cacheCreator = cacheCreator;
  }

  @Override
  public InvNodeRepository createRepo(OClass cls) {
    var cache = cacheCreator.getInvCache(cls);
    return new EIspnInvNodeRepository(cache.getAdvancedCache());
  }

  @Override
  protected OClass load(String clsKey) {
    return classRepo.get(clsKey);
  }
}
