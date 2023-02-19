package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Uni;

import java.util.Collection;

public interface NotifyEntityRepository<K, V> {
  Uni<V> persistAsync(V v, boolean notificationEnabled);
  Uni<Void> persistAsync(Collection<V> collection, boolean notificationEnabled);
}
