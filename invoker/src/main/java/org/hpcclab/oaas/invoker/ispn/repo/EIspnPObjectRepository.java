package org.hpcclab.oaas.invoker.ispn.repo;

import org.hpcclab.oaas.proto.ProtoOObject;
import org.infinispan.AdvancedCache;

public class EIspnPObjectRepository extends AbsEIspnRepository<ProtoOObject> {
  AdvancedCache<String, ProtoOObject> cache;

  public EIspnPObjectRepository(AdvancedCache<String, ProtoOObject> cache) {
    this.cache = cache;
  }

  @Override
  String extractKey(ProtoOObject oObject) {
    return oObject.getId();
  }

  @Override
  public AdvancedCache<String, ProtoOObject> getCache() {
    return cache;
  }
}
