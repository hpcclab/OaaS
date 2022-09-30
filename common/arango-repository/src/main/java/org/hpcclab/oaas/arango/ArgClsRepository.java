package org.hpcclab.oaas.arango;

import com.arangodb.ArangoCollection;
import com.arangodb.async.ArangoCollectionAsync;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.ClassResolver;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ArgClsRepository extends AbstractCachedArgRepository<OaasClass> implements ClassRepository {

  @Inject
  @Named("ClassCollection")
  ArangoCollection collection;
  @Inject
  @Named("ClassCollectionAsync")
  ArangoCollectionAsync collectionAsync;

  @Inject
  ClassResolver classResolver;

  Cache<String, OaasClass> cache;

  @PostConstruct
  void setup() {
    cache = Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(10))
      .build();
  }

  @Override
  public ArangoCollection getCollection() {
    return collection;
  }

  @Override
  public ArangoCollectionAsync getCollectionAsync() {
    return collectionAsync;
  }

  @Override
  public Class<OaasClass> getValueCls() {
    return OaasClass.class;
  }

  @Override
  public String extractKey(OaasClass cls) {
    return cls.getName();
  }

  @Override
  Cache<String, OaasClass> cache() {
    return cache;
  }

  public OaasClass resolveInheritance(OaasClass baseCls, Map<String, OaasClass> clsMap) {
    if (baseCls.getParents()==null || baseCls.getParents().isEmpty())
      return baseCls;
    var parentClasses = baseCls.getParents()
      .stream()
      .map(clsName -> {
        if (clsMap.containsKey(clsName)) return clsMap.get(clsName);
        var cls = get(clsName);
        return resolveInheritance(cls, clsMap);
      })
      .toList();
    clsMap.put(baseCls.getKey(), baseCls);
    return classResolver.merge(baseCls, parentClasses);
  }

  @Override
  public Map<String, OaasClass> resolveInheritance(Map<String, OaasClass> clsMap) {
    var startingClasses = List.copyOf(clsMap.values());
    for (var cls : startingClasses) {
      cls = resolveInheritance(cls, clsMap);
      clsMap.put(cls.getName(), cls);
    }
    return clsMap;
  }


}
