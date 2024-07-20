package org.hpcclab.oaas.arango.repo;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCollectionAsync;
import org.hpcclab.oaas.arango.CacheFactory;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.repository.ClassRepository;

import java.util.List;
import java.util.Map;


public class ArgClsRepository extends AbstractArgRepository<OClass> implements ClassRepository {


  ArangoCollection collection;
  ArangoCollectionAsync collectionAsync;
  CacheFactory cacheFactory;

  public ArgClsRepository(ArangoCollection collection,
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
  public Class<OClass> getValueCls() {
    return OClass.class;
  }

  @Override
  public String extractKey(OClass cls) {
    return cls.getKey();
  }


  @Override
  public List<OClass> listSubCls(String clsKey) {
    var query = """
      FOR cls IN @@col
        FILTER cls.resolved.identities ANY == @key
        return cls
      """;
    return getQueryService().query(query,
      Map.of(
        "@col", getCollection().name(),
        "key", clsKey)
    );
  }
}
