package org.hpcclab.oaas.invoker.ispn.repo;

import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.infinispan.AdvancedCache;

public class EIspnFnRepository extends AbsEIspnRepository<OaasFunction>
  implements FunctionRepository {
  AdvancedCache<String, OaasFunction> cache;

  public EIspnFnRepository(AdvancedCache<String, OaasFunction> cache) {
    this.cache = cache;
  }

  @Override
  public AdvancedCache<String, OaasFunction> getCache() {
    return cache;
  }
}
