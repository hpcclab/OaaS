package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Uni;

public interface CachedEntityRepository<K,V> extends EntityRepository<K,V> {
  void invalidate(K key);
  V getWithoutCache(K key);
  Uni<V> getWithoutCacheAsync(K key);
}
