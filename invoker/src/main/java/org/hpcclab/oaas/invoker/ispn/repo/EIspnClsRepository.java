package org.hpcclab.oaas.invoker.ispn.repo;

import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.repository.ClassRepository;
import org.infinispan.AdvancedCache;

public class EIspnClsRepository extends AbsEIspnRepository<OaasClass>
  implements ClassRepository {
  AdvancedCache<String, OaasClass> cache;

  public EIspnClsRepository(AdvancedCache<String, OaasClass> cache) {
    this.cache = cache;
  }

  @Override
  public AdvancedCache<String, OaasClass> getCache() {
    return cache;
  }
}
