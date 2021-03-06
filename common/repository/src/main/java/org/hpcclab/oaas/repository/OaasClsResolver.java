package org.hpcclab.oaas.repository;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.function.OaasFunctionBinding;
import org.hpcclab.oaas.model.proto.OaasClass;
import org.hpcclab.oaas.repository.mapper.ModelMapper;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class OaasClsResolver {

  @Inject
  OaasClassRepository clsRepo;
  @Inject
  ModelMapper modelMapper;

  @Inject
  @CacheName("resolvedClass")
  Cache cache;


  @CacheResult(cacheName = "resolvedClass")
  public Uni<OaasClass> loadAndResolve(OaasClass cls) {
    if (cls.getParents() == null || cls.getParents().isEmpty()) {
      return Uni.createFrom().item(cls);
    }else {
      return clsRepo.listAsync(Set.copyOf(cls.getParents()))
        .map(parentMap -> cls.getParents()
          .stream()
          .map(parentMap::get)
          .toList()
        )
        .map(parents -> resolve(cls,parents));
    }
  }

  public OaasClass resolve(OaasClass child,
                           List<OaasClass> parents) {
    var fbMap = Stream.concat(parents.stream(), Stream.of(child))
      .flatMap(cls -> cls.getFunctions().stream())
      .collect(Collectors.toMap(OaasFunctionBinding::getFunction, Function.identity()));
    var cls = modelMapper.copy(child);
    cls.setFunctions(Set.copyOf(fbMap.values()));
    return cls;
  }
}
