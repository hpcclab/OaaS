package org.hpcclab.oaas.invoker.ispn.repo;

import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.infinispan.AdvancedCache;

public class EmbeddedIspnFnRepository extends AbsEmbeddedIspnRepository<OaasFunction>
  implements FunctionRepository {
  AdvancedCache<String, OaasFunction> cache;

  public EmbeddedIspnFnRepository(AdvancedCache<String, OaasFunction> cache) {
    this.cache = cache;
  }

  @Override
  AdvancedCache<String, OaasFunction> getCache() {
    return cache;
  }
}
