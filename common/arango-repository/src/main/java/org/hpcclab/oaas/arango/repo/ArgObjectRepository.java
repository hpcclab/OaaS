package org.hpcclab.oaas.arango.repo;

import com.arangodb.ArangoCollection;
import com.arangodb.async.ArangoCollectionAsync;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class ArgObjectRepository extends AbstractArgRepository<OaasObject> implements ObjectRepository {
  private static final Logger LOGGER = LoggerFactory.getLogger(ArgObjectRepository.class);

  ArangoCollection collection;

  ArangoCollectionAsync collectionAsync;

  public ArgObjectRepository(ArangoCollection collection,
                             ArangoCollectionAsync collectionAsync) {
    this.collection = collection;
    this.collectionAsync = collectionAsync;
  }

  @Override
  public ArangoCollection getCollection() {
    return collection;
  }

  @Override
  public ArangoCollectionAsync getAsyncCollection() {
    return collectionAsync;
  }

  @Override
  public Class<OaasObject> getValueCls() {
    return OaasObject.class;
  }

  @Override
  public String extractKey(OaasObject oaasObject) {
    return oaasObject.getId();
  }


  public Uni<Pagination<OaasObject>> listByCls(List<String> clsKeys,
                                               long offset,
                                               int limit) {
    // langauge=AQL
    var query = """
      FOR obj IN @@col
        FILTER obj.cls in @cls
        LIMIT @off, @lim
        RETURN obj
      """;
    return getQueryService().queryPaginationAsync(
      query,
      Map.of("@col", getCollection().name(),
        "cls", clsKeys,
        "off", offset,
        "lim", limit
      ),
      offset,
      limit
    );
  }

  public Uni<Pagination<OaasObject>> sortedListByCls(List<String> clsKeys,
                                                     String sortKey,
                                                     boolean desc,
                                                     long offset,
                                                     int limit) {
    if (LOGGER.isDebugEnabled())
      LOGGER.debug("sortedListByCls {}, {}, {}",
        clsKeys, sortKey, desc);
    // langauge=AQL
    var query = """
      FOR obj IN @@col
        FILTER obj.cls in @cls
        SORT obj.@sort @order
        LIMIT @off, @lim
        RETURN obj
      """;
    return getQueryService().queryPaginationAsync(
      query,
      Map.of("@col", getCollection().name(),
        "cls", clsKeys,
        "order", desc ? "DESC":"ASC",
        "off", offset,
        "lim", limit,
        "sort", sortKey.split("\\.")
      ),
      offset,
      limit
    );
  }
}
