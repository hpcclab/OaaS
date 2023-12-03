package org.hpcclab.oaas.invoker.ispn.lookup;

import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.hpcclab.oaas.model.cls.OClass;

public class LookupManager {
  int defaultSegmentSize = 64;
  ConcurrentMutableMap<String, ObjectLocationLookup> map = new ConcurrentHashMap<>();
  final LocationRegistry registry;

  public LookupManager(LocationRegistry registry) {
    this.registry = registry;
  }

  public LocationRegistry getRegistry() {
    return registry;
  }

  public ObjectLocationLookup getOrInit(OClass cls) {
    return map.computeIfAbsent(
      cls.getKey(),
      k -> new ObjectLocationLookup(cls.getConfig().getPartitions(), cls.getKey(), registry)
    );
  }

  public boolean isLocal(ApiAddress address) {
    return address.host().equals(registry.getLocalhost());
  }
}
