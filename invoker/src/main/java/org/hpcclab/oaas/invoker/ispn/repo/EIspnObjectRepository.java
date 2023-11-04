package org.hpcclab.oaas.invoker.ispn.repo;

import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.infinispan.AdvancedCache;

public class EIspnObjectRepository extends AbsEIspnRepository<OaasObject>
  implements ObjectRepository {
  AdvancedCache<String, OaasObject> cache;

  public EIspnObjectRepository(AdvancedCache<String, OaasObject> cache) {
    this.cache = cache;
  }

  @Override
  public AdvancedCache<String, OaasObject> getCache() {
    return cache;
  }

}
