package org.hpcclab.oaas.invoker.ispn.repo;

import org.hpcclab.oaas.model.object.ObjectInvNode;
import org.infinispan.AdvancedCache;

public class EmbeddedIspnInvNodeRepository extends AbsEmbeddedIspnRepository<ObjectInvNode> {

  AdvancedCache<String, ObjectInvNode> cache;

  public EmbeddedIspnInvNodeRepository(AdvancedCache<String, ObjectInvNode> cache) {
    this.cache = cache;
  }

  @Override
  AdvancedCache<String, ObjectInvNode> getCache() {
    return cache;
  }


}
