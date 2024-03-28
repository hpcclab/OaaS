package org.hpcclab.oaas.arango.repo;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCollectionAsync;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class ArgObjectRepository extends AbstractArgRepository<OObject> implements ObjectRepository {
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
  public Class<OObject> getValueCls() {
    return OObject.class;
  }

  @Override
  public String extractKey(OObject oObject) {
    return oObject.getId();
  }


  public Uni<Pagination<OObject>> listByCls(List<String> clsKeys,
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

  public Uni<Pagination<OObject>> sortedListByCls(List<String> clsKeys,
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
