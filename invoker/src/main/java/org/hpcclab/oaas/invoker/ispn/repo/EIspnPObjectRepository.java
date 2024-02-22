package org.hpcclab.oaas.invoker.ispn.repo;

import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.proto.ProtoOObject;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.infinispan.AdvancedCache;

import java.util.function.BiFunction;

public class EIspnPObjectRepository extends AbsEIspnRepository<ProtoOObject>{
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
