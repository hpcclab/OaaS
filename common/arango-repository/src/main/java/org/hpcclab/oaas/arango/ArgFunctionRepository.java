package org.hpcclab.oaas.arango;

import com.arangodb.ArangoCollection;
import com.arangodb.async.ArangoCollectionAsync;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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

  Cache<String, OaasFunction> cache;

  @PostConstruct
  void setup() {
    cache = Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(10))
      .build();
  }
  @Override
  ArangoCollection getCollection() {
    return collection;
  }

  @Override
  ArangoCollectionAsync getCollectionAsync() {
    return collectionAsync;
  }

  @Override
  Class<OaasFunction> getValueCls() {
    return OaasFunction.class;
  }

  @Override
  String extractKey(OaasFunction oaasFunction) {
    return oaasFunction.getName();
  }

  @Override
  Cache<String, OaasFunction> cache() {
    return cache;
  }
}
