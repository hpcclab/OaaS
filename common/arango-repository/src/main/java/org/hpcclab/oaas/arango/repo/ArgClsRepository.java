package org.hpcclab.oaas.arango.repo;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCollectionAsync;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.inject.Inject;
import org.hpcclab.oaas.arango.CacheFactory;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.repository.ClassRepository;

import java.util.List;
import java.util.Map;


public class ArgClsRepository extends AbstractCachedArgRepository<OaasClass> implements ClassRepository {


  private final Cache<String, OaasClass> cache;
  private final Cache<String, List<String>> subClsCache;
  ArangoCollection collection;
  ArangoCollectionAsync collectionAsync;
  CacheFactory cacheFactory;

  @Inject
  public ArgClsRepository(ArangoCollection collection,
                          ArangoCollectionAsync collectionAsync,
                          CacheFactory cacheFactory) {
    this.collection = collection;
    this.collectionAsync = collectionAsync;
    this.cacheFactory = cacheFactory;
    cache = cacheFactory.get();
    subClsCache = cacheFactory.getLongTermVer();
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
  public Class<OaasClass> getValueCls() {
    return OaasClass.class;
  }

  @Override
  public String extractKey(OaasClass cls) {
    return cls.getKey();
  }

  @Override
  Cache<String, OaasClass> cache() {
    return cache;
  }

  @Override
  public List<OaasClass> listSubCls(String clsKey) {
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
