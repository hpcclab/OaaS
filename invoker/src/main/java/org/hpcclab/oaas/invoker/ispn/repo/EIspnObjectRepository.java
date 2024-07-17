package org.hpcclab.oaas.invoker.ispn.repo;

import org.hpcclab.oaas.model.object.GOObject;
import org.hpcclab.oaas.model.object.JOObject;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.infinispan.AdvancedCache;

public class EIspnObjectRepository extends AbsEIspnRepository<GOObject>
  implements ObjectRepository {
  AdvancedCache<String, GOObject> cache;

  public EIspnObjectRepository(AdvancedCache<String, GOObject> cache,
                               ArgQueryService<GOObject, JOObject> queryService) {
    this.cache = cache;
    this.queryService = queryService;
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
