package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.Pagination;

import java.util.List;
import java.util.Map;

public interface QueryService<K, V> {
  Pagination<V> pagination(long offset, int limit);
  Uni<Pagination<V>> paginationAsync(long offset, int limit);

  Uni<Pagination<V>> sortedPaginationAsync(String name, boolean desc,long offset, int limit);

  default Pagination<V> queryPagination(String queryString, long offset, int limit) {
    return queryPagination(queryString, Map.of(), offset, limit);
  }

  List<V> query(String queryString, Map<String, Object> params);
  Uni<List<V>> queryAsync(String queryString, Map<String, Object> params);

  Pagination<V> queryPagination(String queryString, Map<String, Object> params, long offset, int limit);
  Uni<Pagination<V>> queryPaginationAsync(String queryString, Map<String, Object> params, long offset, int limit);
}
