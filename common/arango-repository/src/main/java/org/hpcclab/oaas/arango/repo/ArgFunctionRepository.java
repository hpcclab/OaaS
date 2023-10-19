package org.hpcclab.oaas.arango.repo;

import com.arangodb.ArangoCollection;
import com.arangodb.async.ArangoCollectionAsync;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.hpcclab.oaas.arango.CacheFactory;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.repository.FunctionRepository;

public class ArgFunctionRepository extends AbstractCachedArgRepository<OaasFunction> implements FunctionRepository {

  ArangoCollection collection;
  ArangoCollectionAsync collectionAsync;
  CacheFactory cacheFactory;
  private Cache<String, OaasFunction> cache;


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
  public Class<OaasFunction> getValueCls() {
    return OaasFunction.class;
  }

  @Override
  public String extractKey(OaasFunction oaasFunction) {
    return oaasFunction.getKey();
  }

  @Override
  Cache<String, OaasFunction> cache() {
    return cache;
  }
}
