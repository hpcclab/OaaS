package org.hpcclab.oaas.arango;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDBException;
import com.arangodb.async.ArangoCollectionAsync;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.model.*;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.mutiny.core.Vertx;
import org.eclipse.collections.impl.block.factory.Functions;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.repository.EntityRepository;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public abstract class AbstractArgRepository<V>
  implements EntityRepository<String, V> {

  final static DocumentDeleteOptions DELETE_OPTIONS = new DocumentDeleteOptions().returnOld(true);
  final static DocumentCreateOptions CREATE_OPTIONS = new DocumentCreateOptions().overwriteMode(OverwriteMode.replace);
  static final DocumentReplaceOptions REPLACE_OPTIONS = new DocumentReplaceOptions().ignoreRevs(false);
  static final AqlQueryOptions QUERY_OPTIONS = new AqlQueryOptions()
    .fullCount(true);

  abstract ArangoCollection getCollection();

  abstract ArangoCollectionAsync getCollectionAsync();

  abstract Class<V> getValueCls();

  abstract String extractKey(V v);

  @Override
  public V get(String key) {
    Objects.requireNonNull(key);
    return getCollection().getDocument(key, getValueCls());
  }

  @Override
  public Uni<V> getAsync(String key) {
    Objects.requireNonNull(key);
    var future = getCollectionAsync()
      .getDocument(key, getValueCls());
    return createUni(future);
  }

  @Override
  public Map<String, V> list(Collection<String> keys) {
    var multiDocument = getCollection().getDocuments(keys, getValueCls());
    return multiDocument.getDocuments()
      .stream()
      .collect(Collectors.toMap(this::extractKey, Functions.identity()));
  }

  @Override
  public Uni<Map<String, V>> listAsync(Collection<String> keys) {
    var future = getCollectionAsync().getDocuments(keys, getValueCls());
    return createUni(future)
      .map(multiDocument -> multiDocument.getDocuments()
        .stream()
        .collect(Collectors.toMap(this::extractKey, Functions.identity()))
      );
  }

  @Override
  public V remove(String key) {
    var deleteEntity = getCollection().deleteDocument(key, getValueCls(), DELETE_OPTIONS);
    return deleteEntity.getOld();
  }

  @Override
  public Uni<V> removeAsync(String key) {
    var future = getCollectionAsync().deleteDocument(key, getValueCls(), DELETE_OPTIONS);
    return createUni(future)
      .map(DocumentDeleteEntity::getOld);
  }

  @Override
  public V put(String key, V value) {
    var doc = getCollection().insertDocument(value, CREATE_OPTIONS);
    return value;
  }

  @Override
  public Uni<V> putAsync(String key, V value) {
    var future = getCollectionAsync().insertDocument(value, CREATE_OPTIONS);
    return createUni(future)
      .replaceWith(value);
  }

  @Override
  public Uni<V> persistAsync(V v,
                             boolean notificationEnabled) {
    var k = extractKey(v);
    return putAsync(k, v);
  }

  @Override
  public Uni<Void> persistAsync(Collection<V> collection,
                                boolean notificationEnabled) {
    var future = getCollectionAsync().insertDocuments(collection, CREATE_OPTIONS);
    return createUni(future)
      .invoke(Unchecked.consumer(mde -> {
        if (mde.getErrors().size() > 0) {
          throw new DataAccessException(mde.getErrors());
        }
      }))
      .replaceWithVoid();
  }

  @Override
  public Uni<V> computeAsync(String key, BiFunction<String, V, V> function) {
    var uni = Uni.createFrom().completionStage(() -> {
        var doc = get(key);
        var newDoc = function.apply(key, doc);
        return getCollectionAsync().replaceDocument(key, newDoc, REPLACE_OPTIONS)
          .thenApply(__ -> newDoc);
      })
      .onFailure(ArangoDBException.class)
      .retry().atMost(5);
    var ctx = Vertx.currentContext();
    if (ctx!=null)
      return uni.emitOn(ctx::runOnContext);
    return uni;
  }

  @Override
  public Pagination<V> query(String queryString, Map<String, Object> params, long offset, int limit) {
    var cursor = getCollection().db()
      .query(queryString, params, QUERY_OPTIONS, getValueCls());
    try (cursor) {
      var items = cursor.asListRemaining();
      return new Pagination<>(
        cursor.getStats().getFullCount(),
        offset,
        limit, items);
    } catch (IOException e) {
      throw new DataAccessException(e);
    }
  }

  @Override
  public Pagination<V> pagination(long offset, int limit) {
    // langauge=AQL
    var query = """
      FOR doc IN @@col
        LIMIT @off, @lim
        RETURN doc
      """;
    return query(query,
      Map.of("@col", getCollection().name(),
        "off",offset,
        "lim",limit),
      offset,
      limit
    );
  }

  protected <T> Uni<T> createUni(CompletionStage<T> stage) {
    var uni = Uni.createFrom().completionStage(stage);
    var ctx = Vertx.currentContext();
    if (ctx!=null)
      return uni.emitOn(ctx::runOnContext);
    return uni;
  }


}
