package org.hpcclab.oaas.invoker.ispn.repo;

import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.repository.ClassRepository;
import org.infinispan.AdvancedCache;

public class EIspnClsRepository extends AbsEIspnRepository<OClass>
  implements ClassRepository {
  AdvancedCache<String, OClass> cache;

  public EIspnClsRepository(AdvancedCache<String, OClass> cache) {
    this.cache = cache;
  }

  @Override
  public AdvancedCache<String, OClass> getCache() {
    return cache;
  }
}
