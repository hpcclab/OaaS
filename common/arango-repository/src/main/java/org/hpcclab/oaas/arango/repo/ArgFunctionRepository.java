package org.hpcclab.oaas.arango.repo;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCollectionAsync;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.PostConstruct;
import org.hpcclab.oaas.arango.CacheFactory;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.repository.FunctionRepository;

public class ArgFunctionRepository extends AbstractCachedArgRepository<OFunction> implements FunctionRepository {

  ArangoCollection collection;
  ArangoCollectionAsync collectionAsync;
  CacheFactory cacheFactory;
  private Cache<String, OFunction> cache;


  public ArgFunctionRepository(ArangoCollection collection,
                               ArangoCollectionAsync collectionAsync,
                               CacheFactory cacheFactory) {
    this.collection = collection;
    this.collectionAsync = collectionAsync;
    this.cacheFactory = cacheFactory;
    cache = cacheFactory.get();
  }

  @PostConstruct
  void setup() {
    cache = cacheFactory.get();
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
  public Class<OFunction> getValueCls() {
    return OFunction.class;
  }

  @Override
  public String extractKey(OFunction oaasFunction) {
    return oaasFunction.getKey();
  }

  @Override
  Cache<String, OFunction> cache() {
    return cache;
  }
}
