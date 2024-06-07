package org.hpcclab.oaas.invoker.ispn.repo;

import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.model.object.POObject;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.infinispan.AdvancedCache;

public class EIspnObjectRepository extends AbsEIspnRepository<POObject>
  implements ObjectRepository {
  AdvancedCache<String, POObject> cache;

  public EIspnObjectRepository(AdvancedCache<String, POObject> cache) {
    this.cache = cache;
  }

  @Override
  String extractKey(POObject oObject) {
    return oObject.getKey();
  }

  @Override
  public AdvancedCache<String, POObject> getCache() {
    return cache;
  }

}
