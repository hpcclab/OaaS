package org.hpcclab.oaas.ispn.repo;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.repository.CachedEntityRepository;
import org.hpcclab.oaas.repository.NotifyEntityRepository;
import org.hpcclab.oaas.repository.QueryService;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractIspnRepository<K, V> implements CachedEntityRepository<K,V>, NotifyEntityRepository<K,V> {
  private static final Logger LOGGER = LoggerFactory.getLogger( AbstractIspnRepository.class );

  protected IspnQueryService<K,V> queryService;

  public abstract RemoteCache<K, V> getRemoteCache();



  public abstract String getEntityName();



  public V get(K key) {
    Objects.requireNonNull(key);
    return getRemoteCache().get(key);
  }

  public Uni<V> getAsync(K key) {
    Objects.requireNonNull(key);
    var ctx = Vertx.currentContext();
    var uni = Uni.createFrom().completionStage(getRemoteCache().getAsync(key));
    if (ctx!=null)
      uni = uni.emitOn(ctx::runOnContext);
    return uni;
  }

//  public Uni<MetadataValue<V>> getWithMetaAsync(K key) {
//    Objects.requireNonNull(key);
//    var ctx = Vertx.currentContext();
//    var uni = Uni.createFrom().completionStage(getRemoteCache().getWithMetadataAsync(key));
//    if (ctx!=null)
//      uni = uni.emitOn(ctx::runOnContext);
//    return uni;
//  }

  public Map<K, V> list(Collection<K> keys) {
    Set<K> keySet = keys instanceof Set<K>? (Set<K>) keys: Set.copyOf(keys);
    return getRemoteCache().getAll(keySet);
  }

  public Uni<Map<K, V>> listAsync(Collection<K> keys) {
    Set<K> keySet = keys instanceof Set<K>? (Set<K>) keys: Set.copyOf(keys);
    var ctx = Vertx.currentContext();
    var uni = Uni.createFrom().completionStage(getRemoteCache()
      .getAllAsync(keySet));
    if (ctx!=null)
      uni = uni.emitOn(ctx::runOnContext);
    return uni;
  }

  public V put(K key, V value) {
    Objects.requireNonNull(key);
    getRemoteCache().putAsync(key, value);
    return value;
  }

  public Uni<V> putAsync(K key, V value) {
    Objects.requireNonNull(key);
    var ctx = Vertx.currentContext();
    var uni = Uni.createFrom().completionStage(getRemoteCache().putAsync(key, value))
      .replaceWith(value);
    if (ctx!=null)
      uni = uni.emitOn(ctx::runOnContext);
    return uni;
  }

  public Uni<Void> putAllAsync(Map<K, V> map) {
    Objects.requireNonNull(map);
    var ctx = Vertx.currentContext();
    var uni = Uni.createFrom().completionStage(getRemoteCache().putAllAsync(map));
    if (ctx!=null)
      uni = uni.emitOn(ctx::runOnContext);
    return uni;
  }

  @Override
  public V remove(K key) {
    return getRemoteCache().remove(key);
  }

  public Uni<V> removeAsync(K key) {
    Objects.requireNonNull(key);
    var ctx = Vertx.currentContext();
    var uni = Uni.createFrom().completionStage(getRemoteCache().removeAsync(key));
    if (ctx!=null)
      uni = uni.emitOn(ctx::runOnContext);
    return uni;
  }

  public Uni<V> computeAsync(K key, BiFunction<K, V, V> function) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(function);
//    if (LOGGER.isInfoEnabled()){
//      LOGGER.info("computeAsync {} {}", getEntityName(), key);
//    }
    var ctx = Vertx.currentContext();
    var uni = Uni.createFrom()
      .completionStage(getRemoteCache().computeAsync(key,function));
    if (ctx!=null)
      uni = uni.emitOn(ctx::runOnContext);
    return uni;
  }

  public V persist(V v) {
    Objects.requireNonNull(v);
    K k = extractKey(v);
    Objects.requireNonNull(k);
    return this.put(k, v);
  }

  public Uni<V> persistAsync(V v) {
    Objects.requireNonNull(v);
    K k = extractKey(v);
    Objects.requireNonNull(k);
    return this.putAsync(k, v);
  }

  @Override
  public Uni<V> persistAsync(V v, boolean notificationEnabled) {
    if (!notificationEnabled) {
      getRemoteCache().withFlags(Flag.SKIP_LISTENER_NOTIFICATION);
    }
    Objects.requireNonNull(v);
    K k = extractKey(v);
    Objects.requireNonNull(k);
    return this.putAsync(k, v);
  }

  public Uni<Void> persistAsync(Collection<V> collection) {
    var map = collection.stream()
      .collect(Collectors.toMap(this::extractKey, Function.identity()));
    return this.putAllAsync(map);
  }

  @Override
  public Uni<Void> persistAsync(Collection<V> collection,
                                boolean notificationEnabled) {
    if (!notificationEnabled) {
      getRemoteCache().withFlags(Flag.SKIP_LISTENER_NOTIFICATION);
    }
    var map = collection.stream()
      .collect(Collectors.toMap(this::extractKey, Function.identity()));
//    if (LOGGER.isInfoEnabled()){
//      LOGGER.info("persist {} {}", getEntityName(), Json.encode(collection));
//    }
    return this.putAllAsync(map);
  }

  @Override
  public V compute(K key, BiFunction<K, V, V> function) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(function);
    var ctx = Vertx.currentContext();
    return getRemoteCache().compute(key,function);
  }

  protected abstract K extractKey(V v);



  @Override
  public void invalidate(K key) {
  }

  @Override
  public V getBypassCache(K key) {
    return this.get(key);
  }

  @Override
  public Uni<V> getBypassCacheAsync(K key) {
    return this.getAsync(key);
  }

  @Override
  public IspnQueryService<K,V> getQueryService() {
    if (queryService == null)
      queryService = new IspnQueryService<>(this);
    return queryService;
  }
}
