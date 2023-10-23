package org.hpcclab.oaas.invoker.ispn.lookup;

import org.infinispan.commons.hash.Hash;
import org.infinispan.commons.hash.MurmurHash3;

public class ObjectLocationLookup {
    final int segments;
    final int size;
    final Hash hashFunction = MurmurHash3.getInstance();
    String cls;
    LocationRegistry registry;

    public ObjectLocationLookup(int segments,
                                String cls,
                                LocationRegistry registry) {
        this.segments = segments;
        this.registry = registry;
        this.cls = cls;
        size = (int) Math.ceil((double) (1L << 31) / segments);
    }

    public ApiAddress resolve(String id) {
        var seg = (hashFunction.hash(id) & Integer.MAX_VALUE) / size;
        return registry.get(cls, seg);
    }


}
