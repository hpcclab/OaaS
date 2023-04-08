package org.hpcclab.oaas.invoker.ispn.repo;

import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.infinispan.AdvancedCache;

public class EmbeddedIspnObjectRepository extends AbsEmbeddedIspnRepository<OaasObject>
implements ObjectRepository {
  AdvancedCache<String, OaasObject> cache;

  public EmbeddedIspnObjectRepository(AdvancedCache<String, OaasObject> cache) {
    this.cache = cache;
  }

  @Override
  AdvancedCache<String, OaasObject> getCache() {
    return cache;
  }
}
