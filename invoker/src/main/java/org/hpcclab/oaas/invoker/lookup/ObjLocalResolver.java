package org.hpcclab.oaas.invoker.lookup;

import org.hpcclab.oaas.model.cr.CrHash;
import org.infinispan.commons.hash.Hash;
import org.infinispan.commons.hash.MurmurHash3;

import java.util.function.Supplier;

public class ObjLocalResolver {
  final int segments;
  final int size;
  final Hash hashFunction = MurmurHash3.getInstance();
  final String cls;
  final HashRegistry registry;
  int count = 0;

  public ObjLocalResolver(int segments,
                          String cls,
                          HashRegistry registry) {
    this.segments = segments;
    this.registry = registry;
    this.cls = cls;
    size = (int) Math.ceil((double) (1L << 31) / segments);
  }

  public CrHash.ApiAddress find(String id) {
    if (id == null || id.isEmpty()) return getAny();
    var seg = (hashFunction.hash(id) & Integer.MAX_VALUE) / size;
    return registry.get(cls, seg);
  }

  public Supplier<CrHash.ApiAddress> createSupplier(String id){
    if (id == null || id.isEmpty()) return this::getAny;
    var hash = hashFunction.hash(id);
    var seg = (hash & Integer.MAX_VALUE) / size;
    return () -> registry.get(cls, seg);
  }

  public CrHash.ApiAddress getAny() {
    // pseudo RR
    int i = (count++) % (segments);
    return registry.get(cls, i);
  }
}
