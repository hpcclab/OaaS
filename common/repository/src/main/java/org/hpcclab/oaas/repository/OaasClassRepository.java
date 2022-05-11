package org.hpcclab.oaas.repository;

import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.proto.OaasSchema;
import org.hpcclab.oaas.repository.mapper.ModelMapper;
import org.hpcclab.oaas.model.cls.DeepOaasClass;
import org.hpcclab.oaas.model.function.DeepOaasFunctionBinding;
import org.hpcclab.oaas.model.function.OaasFunctionBinding;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class OaasClassRepository extends AbstractIfnpRepository<String, OaasClass> {

  private static final Logger LOGGER = LoggerFactory.getLogger(OaasClassRepository.class);
  private static final String NAME = OaasSchema.makeFullName(OaasClass.class);

  @Inject
  @Remote("OaasClass")
  RemoteCache<String, OaasClass> cache;
  @Inject
  OaasFuncRepository funcRepo;
  @Inject
  ModelMapper oaasMapper;

  @PostConstruct
  void setup() {
    setRemoteCache(cache);
  }

//  @Override
//  public RemoteCache<String, OaasClass> getRemoteCache() {
//    return remoteCache;
//  }

  @Override
  public String getEntityName() {
    return NAME;
  }


  public Uni<DeepOaasClass> getDeep(String name) {
    return getAsync(name)
      .flatMap(cls -> {
        var funcNames = cls.getFunctions()
          .stream()
          .map(OaasFunctionBinding::getFunction)
          .collect(Collectors.toSet());
        return funcRepo.listAsync(funcNames)
          .map(funcMap -> {
            var deepCls = oaasMapper.deep(cls);
            var fbSet =cls.getFunctions().stream()
              .map(fb -> new DeepOaasFunctionBinding()
                .setAccess(fb.getAccess())
                .setFunction(funcMap.get(fb.getFunction()))
              ).collect(Collectors.toSet());
            deepCls.setFunctions(fbSet);
            return deepCls;
          });
      });
  }

  public Uni<OaasClass> persist(OaasClass cls) {
    cls.validate();
    return this.putAsync(cls.getName(), cls);
  }

  public Uni<Void> persist(Collection<OaasClass> classList) {
    classList.forEach(OaasClass::validate);
    var map = classList.stream()
      .collect(Collectors.toMap(OaasClass::getName, Function.identity()));
    return this.putAllAsync(map);
  }


  public Optional<OaasFunctionBinding> findFunction(OaasClass cls, String funcName) {
    return cls.getFunctions()
      .stream()
      .filter(fb -> funcName.equals(fb.getName()) || funcName.equals(fb.getFunction()))
      .findFirst();
  }
}
