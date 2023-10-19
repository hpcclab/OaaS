package org.hpcclab.oaas.arango.repo;

import com.arangodb.ArangoCollection;
import com.arangodb.async.ArangoCollectionAsync;
import com.github.benmanes.caffeine.cache.Cache;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.hpcclab.oaas.arango.CacheFactory;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.exception.OaasValidationException;
import org.hpcclab.oaas.repository.AsyncEntityRepository;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.ClassResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.List;
import java.util.Map;
import java.util.Set;


public class ArgClsRepository extends AbstractCachedArgRepository<OaasClass> implements ClassRepository {


  ArangoCollection collection;
  ArangoCollectionAsync collectionAsync;
  CacheFactory cacheFactory;
  private final Cache<String, OaasClass> cache;
  private final Cache<String, List<String>> subClsCache;

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
  public Uni<List<String>> listSubClsKeys(String clsKey) {
    var res = subClsCache.getIfPresent(clsKey);
    if (res != null)
      return Uni.createFrom().item(res);
    var query = """
      FOR cls IN @@col
        FILTER cls.resolved.identities ANY == @key
        return cls._key
      """;
    var param = Map.<String, Object>of(
      "@col", getCollection().name(),
      "key", clsKey
    );
    return this.queryService.queryAsync(query, String.class, param)
      .invoke(l -> subClsCache.put(clsKey, l));
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
