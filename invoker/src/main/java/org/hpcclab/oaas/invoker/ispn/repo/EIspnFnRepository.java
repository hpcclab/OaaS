package org.hpcclab.oaas.invoker.ispn.repo;

import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.infinispan.AdvancedCache;

public class EIspnFnRepository extends AbsEIspnRepository<OFunction>
  implements FunctionRepository {
  AdvancedCache<String, OFunction> cache;

  public EIspnFnRepository(AdvancedCache<String, OFunction> cache) {
    this.cache = cache;
  }

  @Override
  public AdvancedCache<String, OFunction> getCache() {
    return cache;
  }
}
