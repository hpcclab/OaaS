package org.hpcclab.oaas.ispn.repo;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.repository.QueryService;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;

import java.util.List;
import java.util.Map;

public class IspnQueryService<K,V> implements QueryService<K,V> {

  AbstractIspnRepository<K,V> repo;
  QueryFactory queryFactory;

  public IspnQueryService(AbstractIspnRepository<K, V> repo) {
    this.repo = repo;
  }

  QueryFactory getQueryFactory(){
    if (queryFactory == null)
      this.queryFactory = Search.getQueryFactory(repo.getRemoteCache());
    return this.queryFactory;
  }

  public Pagination<V> pagination(long offset, int limit) {
    return queryPagination("FROM " + repo.getEntityName(), offset, limit);
  }

  public Pagination<V> queryPagination(String queryString, Map<String, Object> params, long offset, int limit) {
    Query<V> query = (Query<V>) getQueryFactory().create(queryString)
      .setParameters(params)
      .startOffset(offset)
      .maxResults(limit);
    var qr= query.execute();
    var items = qr.list();
    return new Pagination<>(qr.hitCount().orElse(0), offset, items.size(), items);
  }


  @Override
  public List<V> query(String queryString, Map<String, Object> params) {
    Query<V> query = (Query<V>) getQueryFactory().create(queryString)
      .setParameters(params);
    var qr= query.execute();
    var items = qr.list();
    return items;
  }

  @Override
  public Uni<List<V>> queryAsync(String queryString, Map<String, Object> params) {
    return Uni.createFrom().item(() -> {
      Query<V> query = (Query<V>) getQueryFactory().create(queryString)
        .setParameters(params);
      var qr= query.execute();
      return qr.list();
    });
  }

  @Override
  public Uni<Pagination<V>> paginationAsync(long offset, int limit) {
    throw StdOaasException.notImplemented();
  }

  @Override
  public Uni<Pagination<V>> queryPaginationAsync(String queryString, Map<String, Object> params, long offset, int limit) {
    throw StdOaasException.notImplemented();
  }
  @Override
  public Uni<Pagination<V>> sortedPaginationAsync(String name, boolean desc, long offset, int limit) {
    throw StdOaasException.notImplemented();
  }
}
