package org.hpcclab.oaas.arango.repo;

import com.arangodb.ArangoDBException;
import com.github.benmanes.caffeine.cache.Cache;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.hpcclab.oaas.repository.CachedEntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

public abstract class AbstractCachedArgRepository<V> extends AbstractArgRepository<V>
  implements CachedEntityRepository<String, V> {
  private static final Logger LOGGER = LoggerFactory.getLogger( AbstractCachedArgRepository.class );

  abstract Cache<String, V> cache();

  @Override
  public V get(String key) {
    return cache().get(key, super::get);
  }

  @Override
  public Uni<V> getAsync(String key) {
    var val = cache().getIfPresent(key);
    if (val != null)
      return Uni.createFrom().item(get(key));
    return getBypassCacheAsync(key);
  }


  @Override
  public V getBypassCache(String key) {
    var v =  super.get(key);
    cache().put(key, v);
    return v;
  }

  @Override
  public Uni<V> getBypassCacheAsync(String key) {
    return super.getAsync(key)
      .onItem().ifNotNull()
      .invoke(v -> cache().put(key, v));
  }

  @Override
  public Map<String, V> list(Collection<String> keys) {
    if (keys.isEmpty()) return Map.of();
    return cache().getAll(keys, kl -> super.list((Collection<String>) kl));
  }

  @Override
  public Uni<Map<String, V>> listAsync(Collection<String> keys) {
    if (keys.isEmpty()) return Uni.createFrom().item(Map.of());
    var results = cache().getAllPresent(keys);
    if (results.size() == keys.size()) {
      return Uni.createFrom().item(results);
    }
    var keyToLoad = Sets.mutable.ofAll(keys);
    keyToLoad.removeAll(results.keySet());
    return super.listAsync(keyToLoad)
      .map(loadedResults -> {
        var m = Maps.mutable.ofMap(results);
        m.putAll(loadedResults);
        cache().putAll(loadedResults);
        return m;
      });
  }

  @Override
  public V remove(String key) {
    cache().invalidate(key);
    return super.remove(key);
  }

  @Override
  public Uni<V> removeAsync(String key) {
    cache().invalidate(key);
    return super.removeAsync(key);
  }

  @Override
  public V put(String key, V value) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(value);
    var v = super.put(key, value);
    cache().put(key, v);
    return v;
  }

  @Override
  public Uni<V> putAsync(String key, V value) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(value);
    return super.putAsync(key, value)
      .invoke(v -> cache().put(key, v));
  }

  @Override
  public Uni<V> persistAsync(V v) {
    return super.persistAsync(v)
      .invoke(val -> cache().put(extractKey(val), val));
  }

  @Override
  public Uni<Void> persistAsync(Collection<V> collection) {
    return super.persistAsync(collection)
      .invoke(() -> {
        for (var val: collection) {
          cache().put(extractKey(val), val);
        }
      });
  }

  @Override
  public Uni<V> computeAsync(String key, BiFunction<String, V, V> function) {
    LOGGER.debug("computeAsync(cache)[{}] {}",
      getCollection().name(), key);
    cache().invalidate(key);
    var uni = Uni.createFrom()
      .completionStage(() -> {
        cache().invalidate(key);
        return getAsyncCollection()
            .getDocument(key, getValueCls())
            .thenCompose(doc -> {
              var newDoc = function.apply(key, doc);
              return getAsyncCollection().replaceDocument(key, newDoc, replaceOptions())
                .thenApply(__ -> newDoc);
            });
        })
      .onFailure(ArangoDBException.class)
      .retry().atMost(5)
      .invoke(val -> cache().put(key, val));
    var ctx = Vertx.currentContext();
    if (ctx!=null)
      return uni.emitOn(ctx::runOnContext);
    return uni;
  }

  @Override
  public V compute(String key, BiFunction<String, V, V> function) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("compute(cache)[{}] {}", getCollection().name(), key);
    }
    var retryCount = 5;
    var col = getCollection();
    ArangoDBException exception = null;
    while (retryCount >0) {
      try {
        cache().invalidate(key);
        var doc = col.getDocument(key,getValueCls());
        var newDoc = function.apply(key, doc);
        col.replaceDocument(key, newDoc, replaceOptions());
        cache().put(key,newDoc);
        return newDoc;
      } catch (ArangoDBException e) {
        exception = e;
      }
      retryCount--;
    }
    throw exception;
  }

  @Override
  public void invalidate(String key) {
    cache().invalidate(key);
  }

  @Override
  public void invalidate(Collection<String> keys) {
    cache().invalidateAll(keys);
  }

}
