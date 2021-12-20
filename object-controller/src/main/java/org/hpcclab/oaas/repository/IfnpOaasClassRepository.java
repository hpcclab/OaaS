package org.hpcclab.oaas.repository;

import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.mapper.OaasMapper;
import org.hpcclab.oaas.model.cls.DeepOaasClassDto;
import org.hpcclab.oaas.model.function.DeepOaasFunctionBindingDto;
import org.hpcclab.oaas.model.proto.OaasClassPb;
import org.hpcclab.oaas.model.proto.OaasFunctionBindingPb;
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
public class IfnpOaasClassRepository extends AbstractIfnpRepository<String, OaasClassPb> {

  private static final Logger LOGGER = LoggerFactory.getLogger(IfnpOaasClassRepository.class);
  private static final String NAME = OaasClassPb.class.getName();

  @Inject
  @Remote("OaasClass")
  RemoteCache<String, OaasClassPb> cache;
  @Inject
  IfnpOaasFuncRepository funcRepo;
  @Inject
  OaasMapper oaasMapper;

  @PostConstruct
  void setup() {
    setRemoteCache(cache);
  }

  @Override
  public RemoteCache<String, OaasClassPb> getRemoteCache() {
    return remoteCache;
  }

  @Override
  public String getEntityName() {
    return NAME;
  }


  public Uni<DeepOaasClassDto> getDeep(String name) {
    return getAsync(name)
      .flatMap(cls -> {
        var funcNames = cls.getFunctions()
          .stream()
          .map(OaasFunctionBindingPb::getFunction)
          .collect(Collectors.toSet());
        return funcRepo.listAsync(funcNames)
          .map(funcMap -> {
            var deepCls = oaasMapper.deep(cls);
            var fbSet =cls.getFunctions().stream()
              .map(fb -> new DeepOaasFunctionBindingDto()
                .setAccess(fb.getAccess())
                .setFunction(funcMap.get(fb.getFunction()))
              ).collect(Collectors.toSet());
            deepCls.setFunctions(fbSet);
            return deepCls;
          });
      });
//    System.out.println("get cls " + name);
//    var cls = getAsync(name);
//    System.out.println("cls " + cls);
//    var funcNames = cls.getFunctions()
//      .stream()
//      .map(OaasFunctionBindingPb::getFunction)
//      .collect(Collectors.toSet());
//    System.out.println("list func " + funcNames);
//    var funcMap = funcRepo.list(funcNames);
//
//    return deepCls;
  }

  public Uni<OaasClassPb> persist(OaasClassPb cls) {
    cls.validate();
    return this.putAsync(cls.getName(), cls);
  }

  public Uni<Void> persist(Collection<OaasClassPb> classList) {
    classList.forEach(OaasClassPb::validate);
    var map = classList.stream()
      .collect(Collectors.toMap(OaasClassPb::getName, Function.identity()));
    return this.putAllAsync(map);
  }


  public Optional<OaasFunctionBindingPb> findFunction(OaasClassPb cls, String funcName) {
    return cls.getFunctions()
      .stream()
      .filter(fb -> fb.getFunction().equals(funcName))
      .findFirst();
  }
}
