package org.hpcclab.oaas.arango.repo;

import com.arangodb.*;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.DocumentDeleteOptions;
import com.arangodb.model.DocumentReplaceOptions;
import com.arangodb.model.OverwriteMode;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.mutiny.core.Vertx;
import org.eclipse.collections.impl.block.factory.Functions;
import org.hpcclab.oaas.arango.ArgDataAccessException;
import org.hpcclab.oaas.arango.MutinyUtils;
import org.hpcclab.oaas.repository.AsyncEntityRepository;
import org.hpcclab.oaas.repository.EntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.hpcclab.oaas.arango.MutinyUtils.createUni;
import static org.hpcclab.oaas.arango.repo.ArgQueryService.queryOptions;

public abstract class AbstractArgRepository<V>
  implements EntityRepository<String, V>, AsyncEntityRepository<String, V> {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractArgRepository.class);

  protected ArgQueryService<V> queryService;
  protected ArgAtomicService<V> atomicService;

  static DocumentReplaceOptions replaceOptions() {
    return new DocumentReplaceOptions()
      .ignoreRevs(false);
  }

  static DocumentCreateOptions createOptions() {
    return new DocumentCreateOptions()
      .overwriteMode(OverwriteMode.replace);
  }

  static DocumentDeleteOptions deleteOptions() {
    return new DocumentDeleteOptions().returnOld(true);
  }

  @Override
  public AsyncEntityRepository<String, V> async() {
    return this;
  }

  public abstract ArangoCollection getCollection();

  public abstract ArangoCollectionAsync getAsyncCollection();

  public abstract Class<V> getValueCls();

  public abstract String extractKey(V v);

  @Override
  public ArgQueryService<V> getQueryService() {
    if (queryService==null)
      queryService = new ArgQueryService<>(this);
    return queryService;
  }

  @Override
  public ArgAtomicService<V> atomic() {
    if (atomicService==null)
      atomicService = new ArgAtomicService<>(this);
    return atomicService;
  }

  @Override
  public V get(String key) {
    Objects.requireNonNull(key);
    if (LOGGER.isDebugEnabled())
      LOGGER.debug("get[{}] {}", getCollection().name(), key);
    return getCollection().getDocument(key, getValueCls());
  }

  @Override
  @Deprecated(forRemoval = true)
  public Multi<V> values() {
    var queryString = """
      FOR doc IN @@col
        RETURN doc
      """;
    Map<String, Object> params = Map.of(
      "@col",
      getCollection().name()
    );
    return MutinyUtils.toMulti(() -> getAsyncCollection()
      .db()
      .query(queryString, getValueCls(), params, queryOptions())
    );
  }


  @Override
  public Uni<V> getAsync(String key) {
    Objects.requireNonNull(key);
    if (LOGGER.isDebugEnabled())
      LOGGER.debug("getAsync[{}] {}", getCollection().name(), key);
    return createUni(() -> getAsyncCollection()
      .getDocument(key, getValueCls()));
  }

  @Override
  public Map<String, V> list(Collection<String> keys) {
    if (LOGGER.isDebugEnabled())
      LOGGER.debug("list[{}] {}", getCollection().name(), keys.size());
    var multiDocument = getCollection().getDocuments(keys, getValueCls());
    return multiDocument.getDocuments()
      .stream()
      .collect(Collectors.toMap(this::extractKey, Functions.identity()));
  }

  @Override
  public Uni<Map<String, V>> listAsync(Collection<String> keys) {
    if (LOGGER.isDebugEnabled())
      LOGGER.debug("listAsync[{}] {}", getCollection().name(),
        keys.size());
    return createUni(() -> getAsyncCollection().getDocuments(keys, getValueCls()))
      .map(multiDocument -> multiDocument.getDocuments()
        .stream()
        .collect(Collectors.toMap(this::extractKey, Functions.identity()))
      );
  }

  @Override
  public V remove(String key) {
    if (LOGGER.isDebugEnabled())
      LOGGER.debug("remove[{}] {}", getCollection().name(), key);
    var deleteEntity = getCollection().deleteDocument(key, deleteOptions(),getValueCls());
    return deleteEntity.getOld();
  }

  @Override
  public Uni<V> removeAsync(String key) {
    if (LOGGER.isDebugEnabled())
      LOGGER.debug("removeAsync[{}] {}", getCollection().name(), key);
    return createUni(() -> getAsyncCollection().deleteDocument(key, deleteOptions(), getValueCls()))
      .map(DocumentDeleteEntity::getOld);
  }

  @Override
  public V put(String key, V value) {
    if (LOGGER.isDebugEnabled())
      LOGGER.debug("put[{}] {}", getCollection().name(), key);
    getCollection().insertDocument(value, createOptions());
    return value;
  }

  @Override
  public V persist(V v) {
    return put(extractKey(v), v);
  }

  @Override
  public Uni<V> putAsync(String key, V value) {
    if (LOGGER.isDebugEnabled())
      LOGGER.debug("putAsync[{}] {}", getCollection().name(), key);
    return createUni(() -> getAsyncCollection().insertDocument(value, createOptions()))
      .replaceWith(value);
  }

  @Override
  public Uni<V> persistAsync(V v) {
    var k = extractKey(v);
    return putAsync(k, v);
  }

  @Override
  public Uni<Void> persistAsync(Collection<V> collection) {
    if (LOGGER.isDebugEnabled())
      LOGGER.debug("persistAsync(col)[{}] {}",
        getCollection().name(), collection.size());
    return createUni(() -> getAsyncCollection()
      .insertDocuments(collection, createOptions()))
      .invoke(Unchecked.consumer(mde -> {
        if (!mde.getErrors().isEmpty()) {
          throw new ArgDataAccessException(mde.getErrors());
        }
      }))
      .replaceWithVoid();
  }

  @Override
  public Uni<V> computeAsync(String key, BiFunction<String, V, V> function) {
    if (LOGGER.isDebugEnabled())
      LOGGER.debug("computeAsync[{}] {}",
        getCollection().name(), key);
    var uni = createUni(() -> getAsyncCollection()
      .getDocument(key, getValueCls())
      .thenCompose(doc -> {
        var newDoc = function.apply(key, doc);
        return getAsyncCollection()
          .replaceDocument(key, newDoc, replaceOptions())
          .thenApply(__ -> newDoc);
      })
    )
      .onFailure(ArangoDBException.class)
      .retry().atMost(5);
    var ctx = Vertx.currentContext();
    if (ctx!=null)
      return uni.emitOn(ctx::runOnContext);
    return uni;
  }

  @Override
  public V compute(String key, BiFunction<String, V, V> function) {
    var retryCount = 5;
    var col = getCollection();
    ArangoDBException exception = null;
    while (retryCount > 0) {
      try {
        var doc = col.getDocument(key, getValueCls());
        var newDoc = function.apply(key, doc);
        col.replaceDocument(key, newDoc, replaceOptions());
        return newDoc;
      } catch (ArangoDBException e) {
        exception = e;
      }
      retryCount--;
    }
    throw exception;
  }


}
