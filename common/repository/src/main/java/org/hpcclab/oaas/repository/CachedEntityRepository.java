package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Uni;

import java.util.Collection;

public interface CachedEntityRepository<K,V> extends EntityRepository<K,V> {
  default void invalidate(K key) {
  }
  default void invalidate(Collection<K> keys) {
  }
  default V getBypassCache(K key){
    return get(key);
  }
  default Uni<V> getBypassCacheAsync(K key){
    return async().getAsync(key);
  }
}
