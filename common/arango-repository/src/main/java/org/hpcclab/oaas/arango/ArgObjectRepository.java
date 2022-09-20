package org.hpcclab.oaas.arango;

import com.arangodb.ArangoCollection;
import com.arangodb.async.ArangoCollectionAsync;
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
  ArangoCollection getCollection() {
    return collection;
  }

  @Override
  ArangoCollectionAsync getCollectionAsync() {
    return collectionAsync;
  }

  @Override
  Class<OaasObject> getValueCls() {
    return OaasObject.class;
  }

  @Override
  String extractKey(OaasObject oaasObject) {
    return oaasObject.getId();
  }

  @Override
  public Pagination<OaasObject> listByCls(String clsName, long offset, int limit) {
    // langauge=AQL
    var query = """
      FOR obj IN @@col
        FILTER obj.cls == @cls
        LIMIT @off, @lim
        RETURN obj
      """;
    return query(
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
