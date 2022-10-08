package org.hpcclab.oaas.arango;

import com.arangodb.ArangoCollection;
import com.arangodb.async.ArangoCollectionAsync;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.ObjectRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

@ApplicationScoped
public class ArgObjectRepository extends AbstractArgRepository<OaasObject> implements ObjectRepository{
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
  public  Class<OaasObject> getValueCls() {
    return OaasObject.class;
  }

  @Override
  public String extractKey(OaasObject oaasObject) {
    return oaasObject.getId();
  }

  @Override
  public Uni<Pagination<OaasObject>> listByCls(String clsName, long offset, int limit) {
    // langauge=AQL
    var query = """
      FOR obj IN @@col
        FILTER obj.cls == @cls
        SORT obj._key
        LIMIT @off, @lim
        RETURN obj
      """;
    return queryPaginationAsync(
      query,
      Map.of("@col", getCollection().name(),
        "cls",clsName,
        "off",offset,
        "lim",limit
        ),
      offset,
      limit
      );
  }
}
