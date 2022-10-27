package org.hpcclab.oaas.arango.repo;

import com.arangodb.ArangoCollection;
import com.arangodb.async.ArangoCollectionAsync;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.hpcclab.oaas.arango.CacheFactory;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.repository.FunctionRepository;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.time.Duration;

@ApplicationScoped
public class ArgFunctionRepository extends AbstractCachedArgRepository<OaasFunction> implements FunctionRepository {

  @Inject
  @Named("FunctionCollection")
  ArangoCollection collection;
  @Inject
  @Named("FunctionCollectionAsync")
  ArangoCollectionAsync collectionAsync;

  @Inject
  CacheFactory cacheFactory;
  private Cache<String, OaasFunction> cache;

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
    return oaasFunction.getName();
  }

  @Override
  Cache<String, OaasFunction> cache() {
    return cache;
  }
}
