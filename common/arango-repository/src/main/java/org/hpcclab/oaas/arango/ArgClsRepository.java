package org.hpcclab.oaas.arango;

import com.arangodb.ArangoCollection;
import com.arangodb.async.ArangoCollectionAsync;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.repository.ClassRepository;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.time.Duration;

@ApplicationScoped
public class ArgClsRepository extends AbstractCachedArgRepository<OaasClass> implements ClassRepository {

  @Inject
  @Named("ClassCollection")
  ArangoCollection collection;
  @Inject
  @Named("ClassCollectionAsync")
  ArangoCollectionAsync collectionAsync;

  Cache<String, OaasClass> cache;

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
  Class<OaasClass> getValueCls() {
    return OaasClass.class;
  }

  @Override
  String extractKey(OaasClass cls) {
    return cls.getName();
  }

  @Override
  Cache<String, OaasClass> cache() {
    return cache;
  }
}
