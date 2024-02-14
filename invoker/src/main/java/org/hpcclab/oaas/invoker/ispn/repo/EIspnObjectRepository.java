package org.hpcclab.oaas.invoker.ispn.repo;

import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.infinispan.AdvancedCache;

public class EIspnObjectRepository extends AbsEIspnRepository<OObject>
  implements ObjectRepository {
  AdvancedCache<String, OObject> cache;

  public EIspnObjectRepository(AdvancedCache<String, OObject> cache) {
    this.cache = cache;
  }

  @Override
  public AdvancedCache<String, OObject> getCache() {
    return cache;
  }

}
