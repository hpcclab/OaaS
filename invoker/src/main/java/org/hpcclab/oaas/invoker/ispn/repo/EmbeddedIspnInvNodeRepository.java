package org.hpcclab.oaas.invoker.ispn.repo;

import org.hpcclab.oaas.invoker.ispn.edge.ObjInvNode;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.infinispan.AdvancedCache;

public class EmbeddedIspnInvNodeRepository extends AbsEmbeddedIspnRepository<ObjInvNode> {

  AdvancedCache<String, ObjInvNode> cache;

  public EmbeddedIspnInvNodeRepository(AdvancedCache<String, ObjInvNode> cache) {
    this.cache = cache;
  }

  @Override
  AdvancedCache<String, ObjInvNode> getCache() {
    return cache;
  }


}
