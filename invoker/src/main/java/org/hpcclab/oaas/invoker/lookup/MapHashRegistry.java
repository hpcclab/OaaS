package org.hpcclab.oaas.invoker.lookup;

import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.hpcclab.oaas.model.cr.CrHash;
import org.hpcclab.oaas.proto.InternalCrStateService;

import java.util.List;
import java.util.Map;

/**
 * @author Pawissanutt
 */
public class MapHashRegistry extends AbstractHashRegistry {

  Map<String, CrHash> map = new ConcurrentHashMap<>();

  public MapHashRegistry(InternalCrStateService crStateService) {
    super(crStateService);
  }

  @Override
  void store(CrHash crHash) {
    map.put(crHash.cls(), crHash);
  }

  @Override
  void storeMerge(CrHash crHash) {
    map.compute(crHash.cls(),
      (k, v) -> {
        if (v==null) return crHash;
        return CrHash.merge(v, crHash);
      });
  }

  public CrHash.ApiAddress get(String cls, int segment) {
    CrHash protoCrHash = map.get(cls);
    if (protoCrHash==null) return null;
    List<CrHash.ApiAddress> apiAddresses = protoCrHash.segmentAddr();
    if (apiAddresses.size() < segment) return null;
    return apiAddresses.get(segment);
  }

  @Override
  CrHash get(String cls) {
    return map.get(cls);
  }

  @Override
  boolean isEmpty() {
    return map.isEmpty();
  }
}
