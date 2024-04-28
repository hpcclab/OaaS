package org.hpcclab.oaas.invoker.lookup;

import org.hpcclab.oaas.invoker.InvokerManager;
import org.hpcclab.oaas.model.cr.CrHash;
import org.hpcclab.oaas.proto.InternalCrStateService;
import org.hpcclab.oaas.proto.ProtoCrHash;
import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Pawissanutt
 */
public class CacheHashRegistry extends AbstractHashRegistry {
  private static final Logger logger = LoggerFactory.getLogger( CacheHashRegistry.class );
  final InvokerManager  invokerManager;
  Cache<String, CrHash> cacheMap;


  public CacheHashRegistry(InternalCrStateService crStateService,
                           InvokerManager invokerManager,
                           Cache<String, CrHash> cacheMap) {
    super(crStateService);
    this.invokerManager = invokerManager;
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
  public void storeExternal(ProtoCrHash protoCrHash) {
    if (invokerManager.getManagedCls().contains(protoCrHash.getCls())) {
      logger.info("update local hash registry '{}'", protoCrHash.getCls());
      var crHash = protoMapper.fromProto(protoCrHash);
      storeMerge(crHash);
    }
  }

  @Override
  boolean isEmpty() {
    return cacheMap.isEmpty();
  }
}
