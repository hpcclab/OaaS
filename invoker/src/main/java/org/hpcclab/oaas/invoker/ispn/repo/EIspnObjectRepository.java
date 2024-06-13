package org.hpcclab.oaas.invoker.ispn.repo;

import org.hpcclab.oaas.model.object.GOObject;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.infinispan.AdvancedCache;

public class EIspnObjectRepository extends AbsEIspnRepository<GOObject>
  implements ObjectRepository {
  AdvancedCache<String, GOObject> cache;

  public EIspnObjectRepository(AdvancedCache<String, GOObject> cache) {
    this.cache = cache;
  }

  @Override
  String extractKey(GOObject oObject) {
    return oObject.getKey();
  }

  @Override
  public AdvancedCache<String, GOObject> getCache() {
    return cache;
  }

}
