package org.hpcclab.oaas.invoker.lookup;

import org.infinispan.commons.hash.Hash;
import org.infinispan.commons.hash.MurmurHash3;

public class HashUtil {
    static final Hash hashFunction = MurmurHash3.getInstance();
    public static int getHashed(String key, int numSegments) {
        var size = (int) Math.ceil((double) (1L << 31) / numSegments);
        return (hashFunction.hash(key) & Integer.MAX_VALUE) / size;
    }
}
