package org.hpcclab.oaas.arango.repo;

import com.arangodb.BaseArangoCursor;
import com.arangodb.model.AqlQueryOptions;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.arango.ArgDataAccessException;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.repository.QueryService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.hpcclab.oaas.arango.MutinyUtils.createUni;

public class ArgQueryService<V> implements QueryService<String, V> {

  AbstractArgRepository<V> repository;

  public ArgQueryService(AbstractArgRepository<V> repository) {
    this.repository = repository;
  }

  static AqlQueryOptions queryOptions() {
    return new AqlQueryOptions();
  }

  @Override
  public Pagination<V> queryPagination(String queryString, Map<String, Object> params, long offset, int limit) {
    var cursor = repository.getCollection().db()
      .query(queryString, repository.getValueCls(), params, queryOptions().fullCount(true));
    try (cursor) {
      var items = cursor.asListRemaining();
      return new Pagination<>(cursor.getStats().getFullCount(), offset, limit, items);
    } catch (IOException e) {
      throw new ArgDataAccessException(e);
    }
  }

  @Override
  public Uni<Pagination<V>> queryPaginationAsync(String queryString, Map<String, Object> params, long offset, int limit) {
    return createUni(() -> repository.getAsyncCollection()
      .db()
      .query(queryString, repository.getValueCls(), params, queryOptions().fullCount(true)
        .batchSize(limit)
      )
      .thenApply(cursor -> {
        var items = cursor.getResult();
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
    return queryPagination(query, Map.of("@col", repository.getCollection().name(), "off", offset, "lim", limit), offset, limit);
  }

  @Override
  public Uni<Pagination<V>> paginationAsync(long offset, int limit) {
    var query = """
      FOR doc IN @@col
        LIMIT @off, @lim
        RETURN doc
      """;
    return queryPaginationAsync(query, Map.of("@col", repository.getCollection().name(), "off", offset, "lim", limit), offset, limit);
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
        "@col", repository.getCollection().name(),
        "sorted", name.split("\\."),
        "off", offset,
        "lim", limit,
        "order", desc ? "DESC":"ASC"
      ),
      offset, limit);
  }

  @Override
  public List<V> query(String queryString, Map<String, Object> params) {
    var cursor = repository.getCollection().db().query(queryString, repository.getValueCls(), params, queryOptions());
    return cursor.asListRemaining();
  }

  @Override
  public Uni<List<V>> queryAsync(String queryString, Map<String, Object> params) {
    return queryAsync(queryString, repository.getValueCls(), params);
  }

  public <T> Uni<List<T>> queryAsync(String queryString, Class<T> resultCls, Map<String, Object> params) {
    return createUni(() -> repository.getAsyncCollection()
      .db()
      .query(queryString, resultCls, params, queryOptions()).thenApply(BaseArangoCursor::getResult)
    );
  }


}
