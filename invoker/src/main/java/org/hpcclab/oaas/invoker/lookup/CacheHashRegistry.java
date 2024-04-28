package org.hpcclab.oaas.invoker.lookup;

import org.hpcclab.oaas.model.cr.CrHash;
import org.hpcclab.oaas.proto.InternalCrStateService;
import org.infinispan.Cache;

import java.util.List;

/**
 * @author Pawissanutt
 */
public class CacheHashRegistry extends AbstractHashRegistry {

  Cache<String, CrHash> cacheMap;

  public CacheHashRegistry(InternalCrStateService crStateService, Cache<String, CrHash> cacheMap) {
    super(crStateService);
    this.cacheMap = cacheMap;
  }

  @Override
  void store(CrHash crHash) {
    cacheMap.put(crHash.cls(), crHash);
  }

  @Override
  void storeMerge(CrHash crHash) {
    cacheMap.compute(crHash.cls(),
      (k, v) -> {
        if (v==null) return crHash;
        return CrHash.merge(v, crHash);
      });

  }

  @Override
  public CrHash.ApiAddress get(String cls, int segment) {
    CrHash protoCrHash = cacheMap.get(cls);
    if (protoCrHash==null) return null;
    List<CrHash.ApiAddress> apiAddresses = protoCrHash.segmentAddr();
    if (apiAddresses.size() < segment) return null;
    return apiAddresses.get(segment);
  }

  @Override
  CrHash get(String cls) {
    return cacheMap.get(cls);
  }

  @Override
  boolean isEmpty() {
    return cacheMap.isEmpty();
  }
}
