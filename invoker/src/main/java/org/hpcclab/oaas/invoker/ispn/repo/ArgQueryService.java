package org.hpcclab.oaas.invoker.ispn.repo;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCollectionAsync;
import com.arangodb.BaseArangoCursor;
import com.arangodb.model.AqlQueryOptions;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import org.hpcclab.oaas.invoker.ispn.store.ValueMapper;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.exception.DataAccessException;
import org.hpcclab.oaas.repository.QueryService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * @author Pawissanutt
 */
public class ArgQueryService<V, S> implements QueryService<String, V> {
  final ArangoCollection collection;
  final ArangoCollectionAsync collectionAsync;
  final ValueMapper<V, S> valueMapper;
  final Class<S> valueCls;

  public ArgQueryService(ArangoCollection collection,
                         ArangoCollectionAsync collectionAsync,
                         Class<S> valueCls,
                         ValueMapper<V, S> valueMapper) {
    this.collection = collection;
    this.collectionAsync = collectionAsync;
    this.valueCls = valueCls;
    this.valueMapper = valueMapper;
  }

  static AqlQueryOptions queryOptions() {
    return new AqlQueryOptions();
  }

  static <T> Uni<T> createUni(Supplier<CompletionStage<T>> stage) {
    var uni = Uni.createFrom().completionStage(stage);
    var ctx = Vertx.currentContext();
    if (ctx!=null)
      return uni.emitOn(ctx::runOnContext);
    return uni;
  }

  @Override
  public Pagination<V> queryPagination(String queryString, Map<String, Object> params, long offset, int limit) {
    var cursor = collection.db()
      .query(queryString, valueCls, params, queryOptions().fullCount(true));
    try (cursor) {
      var items = cursor.asListRemaining()
        .stream().map(valueMapper::mapToCStore)
        .toList();
      return new Pagination<>(cursor.getStats().getFullCount(), offset, limit, items);
    } catch (IOException e) {
      throw new DataAccessException(e);
    }
  }

  @Override
  public Uni<Pagination<V>> queryPaginationAsync(String queryString, Map<String, Object> params, long offset, int limit) {
    return createUni(() -> collectionAsync
      .db()
      .query(queryString, valueCls, params, queryOptions().fullCount(true)
        .batchSize(limit)
      )
      .thenApply(cursor -> {
        var items = cursor.getResult()
          .stream().map(valueMapper::mapToCStore)
          .toList();
        return new Pagination<>(
          cursor.getCount(),
          offset,
          items.size(),
          items
        );
      }));
  }

  @Override
  public Pagination<V> pagination(long offset, int limit) {
    // langauge=AQL
    var query = """
      FOR doc IN @@col
        LIMIT @off, @lim
        RETURN doc
      """;
    return queryPagination(query, Map.of("@col", collection.name(), "off", offset, "lim", limit), offset, limit);
  }

  @Override
  public Uni<Pagination<V>> paginationAsync(long offset, int limit) {
    var query = """
      FOR doc IN @@col
        LIMIT @off, @lim
        RETURN doc
      """;
    return queryPaginationAsync(query, Map.of("@col", collection.name(), "off", offset, "lim", limit), offset, limit);
  }

  @Override
  public Uni<Pagination<V>> sortedPaginationAsync(String name, boolean desc, long offset, int limit) {
    // language=AQL
    var query = """
      FOR doc IN @@col
        SORT doc.@sorted @order
        LIMIT @off, @lim
        RETURN doc
      """;
    return queryPaginationAsync(query, Map.of(
        "@col", collection.name(),
        "sorted", name.split("\\."),
        "off", offset,
        "lim", limit,
        "order", desc ? "DESC":"ASC"
      ),
      offset, limit);
  }

  @Override
  public List<V> query(String queryString, Map<String, Object> params) {
    var cursor = collection.db().query(queryString, valueCls, params, queryOptions());
    return cursor.asListRemaining()
      .stream()
      .map(valueMapper::mapToCStore)
      .toList();
  }

  @Override
  public Uni<List<V>> queryAsync(String queryString, Map<String, Object> params) {
    return queryAsync(queryString, valueCls, params)
      .map(l -> l
        .stream()
        .map(valueMapper::mapToCStore)
        .toList()
      );
  }

  public <T> Uni<List<T>> queryAsync(String queryString, Class<T> resultCls, Map<String, Object> params) {
    params.put("@col", collection.name());
    return createUni(() -> collectionAsync
      .db()
      .query(queryString, resultCls, params, queryOptions())
      .thenApply(BaseArangoCursor::getResult)
    );
  }
}
