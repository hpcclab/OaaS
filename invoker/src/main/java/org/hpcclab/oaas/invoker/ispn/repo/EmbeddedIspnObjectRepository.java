package org.hpcclab.oaas.invoker.ispn.repo;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.infinispan.AdvancedCache;

import java.util.Collection;
import java.util.function.BiFunction;

public class EmbeddedIspnObjectRepository extends AbsEmbeddedIspnRepository<OaasObject>
  implements ObjectRepository {
  AdvancedCache<String, OaasObject> cache;

  public EmbeddedIspnObjectRepository(AdvancedCache<String, OaasObject> cache) {
    this.cache = cache;
  }

  @Override
  AdvancedCache<String, OaasObject> getCache() {
    return cache;
  }

  @Override
  public OaasObject put(String key, OaasObject value) {
    if (value.getState()!=null) {
      value.getState().replaceImmutableMap();
    }
    return super.put(key, value);
  }

  @Override
  public Uni<OaasObject> putAsync(String key, OaasObject value) {
    if (value.getState()!=null) {
      value.getState().replaceImmutableMap();
    }
    return super.putAsync(key, value);
  }

  @Override
  public Uni<Void> persistAsync(Collection<OaasObject> collection) {
    for (var obj : collection) {
      if (obj.getState()!=null) {
        obj.getState().replaceImmutableMap();
      }
    }
    return super.persistAsync(collection);
  }

  @Override
  public Uni<OaasObject> computeAsync(String key, BiFunction<String, OaasObject, OaasObject> function) {
    return super.computeAsync(key, (k, v) -> {
      var obj = function.apply(k, v);
      if (obj.getState()!=null) {
        obj.getState().replaceImmutableMap();
      }
      return obj;
    });
  }
}
