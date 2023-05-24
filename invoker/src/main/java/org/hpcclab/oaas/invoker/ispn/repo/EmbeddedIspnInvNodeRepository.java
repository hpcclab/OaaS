package org.hpcclab.oaas.invoker.ispn.repo;

import org.hpcclab.oaas.model.invocation.InvocationNode;
import org.hpcclab.oaas.repository.InvNodeRepository;
import org.infinispan.AdvancedCache;

public class EmbeddedIspnInvNodeRepository extends AbsEmbeddedIspnRepository<InvocationNode>
implements InvNodeRepository {

  AdvancedCache<String, InvocationNode> cache;

  public EmbeddedIspnInvNodeRepository(AdvancedCache<String, InvocationNode> cache) {
    this.cache = cache;
  }

  @Override
  AdvancedCache<String, InvocationNode> getCache() {
    return cache;
  }


}
