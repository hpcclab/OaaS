package org.hpcclab.oaas.arango.repo;

import com.arangodb.ArangoCollection;
import com.arangodb.async.ArangoCollectionAsync;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@RegisterForReflection(
  targets = {
    OaasObject.class
  },
  registerFullHierarchy = true
)
public class ArgObjectRepository extends AbstractArgRepository<OaasObject> implements ObjectRepository {
  private static final Logger LOGGER = LoggerFactory.getLogger( ArgObjectRepository.class );
  @Inject
  @Named("ObjectCollection")
  ArangoCollection collection;
  @Inject
  @Named("ObjectCollectionAsync")
  ArangoCollectionAsync collectionAsync;

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

  @Override
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
    return queryPaginationAsync(
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
    return queryPaginationAsync(
      query,
      Map.of("@col", getCollection().name(),
        "cls", clsKeys,
        "order",  desc? "DESC" : "ASC",
        "off", offset,
        "lim", limit,
        "sort", sortKey.split("\\.")
      ),
      offset,
      limit
    );
  }
}
