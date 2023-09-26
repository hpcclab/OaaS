package org.hpcclab.oaas.invoker.ispn.repo;

import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.ClassRepository;
import org.infinispan.AdvancedCache;
import org.infinispan.notifications.cachelistener.annotation.TopologyChanged;
import org.infinispan.notifications.cachelistener.event.TopologyChangedEvent;

import java.util.Set;

public class EmbeddedIspnClsRepository extends AbsEmbeddedIspnRepository<OaasClass>
implements ClassRepository {
  AdvancedCache<String, OaasClass> cache;

  public EmbeddedIspnClsRepository(AdvancedCache<String, OaasClass> cache) {
    this.cache = cache;
  }

  @Override
  AdvancedCache<String, OaasClass> getCache() {
    return cache;
  }
}
