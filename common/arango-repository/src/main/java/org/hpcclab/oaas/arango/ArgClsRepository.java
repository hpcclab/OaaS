package org.hpcclab.oaas.arango;

import com.arangodb.ArangoCollection;
import com.arangodb.async.ArangoCollectionAsync;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.exception.OaasValidationException;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.ClassResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class ArgClsRepository extends AbstractCachedArgRepository<OaasClass> implements ClassRepository {
  private static final Logger LOGGER = LoggerFactory.getLogger( ArgClsRepository.class );

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
  public ArangoCollectionAsync getAsyncCollection() {
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

  public List<OaasClass> loadChildren(OaasClass cls) {
    var query = """
      FOR cls IN @@col
        FILTER cls.resolved.identities ANY == @name
        return cls
      """;
    return query(query,
      Map.of(
        "@col", getCollection().name(),
        "name", cls.getName())
    );
  }

  public OaasClass resolveInheritance(OaasClass baseCls,
                                      Map<String, OaasClass> clsMap,
                                      Set<String> path) {
    if (path.contains(baseCls.getName())){
      throw OaasValidationException.errorClassCyclicInheritance(path);
    }
    path.add(baseCls.getName());
    if (baseCls.getParents() ==null) baseCls.setParents(List.of());
    var parentClasses = baseCls.getParents()
      .stream()
      .map(clsName -> {
        OaasClass cls;
        if (clsMap.containsKey(clsName))
          cls = clsMap.get(clsName);
        else
          cls = get(clsName);
        if (!cls.getResolved().isFlag()) {
          cls = resolveInheritance(cls, clsMap, path);
        }
        return cls;
      })
      .toList();
    var newCls = classResolver.resolve(baseCls, parentClasses);
    clsMap.put(baseCls.getKey(), newCls);
    return newCls;
  }

  public boolean checkCycle(OaasClass baseCls, OaasClass parent) {
    if (parent.getResolved().getIdentities()==null
      || parent.getResolved().getIdentities().isEmpty())
      return false;
    return (parent.getResolved().getIdentities().contains(baseCls.getName()));
  }

  @Override
  public Map<String, OaasClass> resolveInheritance(Map<String, OaasClass> clsMap) {
    var startingClasses = List.copyOf(clsMap.values());
    var ctxMap = Maps.mutable.ofMap(clsMap);
    for (var cls : startingClasses) {
      cls.getResolved().setFlag(false);
      resolveInheritance(cls, ctxMap, Sets.mutable.empty());
    }
    for (var cls : startingClasses) {
      var children = loadChildren(cls);
      for (var child: children) {
        child.getResolved().setFlag(false);
        resolveInheritance(child, ctxMap, Sets.mutable.empty());
      }
    }
    return ctxMap;
  }
}
