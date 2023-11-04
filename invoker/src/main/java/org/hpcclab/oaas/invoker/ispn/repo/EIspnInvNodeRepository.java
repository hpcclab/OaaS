package org.hpcclab.oaas.invoker.ispn.repo;

import org.hpcclab.oaas.model.invocation.InvocationNode;
import org.hpcclab.oaas.repository.InvNodeRepository;
import org.infinispan.AdvancedCache;

public class EIspnInvNodeRepository extends AbsEIspnRepository<InvocationNode>
implements InvNodeRepository {

  AdvancedCache<String, InvocationNode> cache;

  public EIspnInvNodeRepository(AdvancedCache<String, InvocationNode> cache) {
    this.cache = cache;
  }

  @Override
  public AdvancedCache<String, InvocationNode> getCache() {
    return cache;
  }


}
