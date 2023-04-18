package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Uni;

public interface CachedEntityRepository<K,V> extends EntityRepository<K,V> {
  default void invalidate(K key) {
  }
  default V getBypassCache(K key){
    return get(key);
  }
  default Uni<V> getBypassCacheAsync(K key){
    return getAsync(key);
  }
}
