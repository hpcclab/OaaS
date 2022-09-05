package org.hpcclab.oaas.repository.impl;

import io.quarkus.infinispan.client.Remote;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.proto.OaasSchema;
import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class OaasClassRepository extends AbstractIfnpRepository<String, OaasClass> {

  private static final Logger LOGGER = LoggerFactory.getLogger(OaasClassRepository.class);
  private static final String NAME = OaasSchema.makeFullName(OaasClass.class);

  @Inject
  @Remote("OaasClass")
  RemoteCache<String, OaasClass> cache;

  @PostConstruct
  void setup() {
    setRemoteCache(cache);
  }

  @Override
  public String getEntityName() {
    return NAME;
  }

  @Override
  protected String extractKey(OaasClass oaasClass) {
    return oaasClass.getName();
  }

//  public Uni<DeepOaasClass> getDeep(String name) {
//    return getAsync(name)
//      .flatMap(cls -> {
//        var funcNames = cls.getFunctions()
//          .stream()
//          .map(OaasFunctionBinding::getFunction)
//          .collect(Collectors.toSet());
//        return funcRepo.listAsync(funcNames)
//          .map(funcMap -> {
//            var deepCls = oaasMapper.deep(cls);
//            var fbSet =cls.getFunctions().stream()
//              .map(fb -> new DeepOaasFunctionBinding()
//                .setAccess(fb.getAccess())
//                .setFunction(funcMap.get(fb.getFunction()))
//              ).collect(Collectors.toSet());
//            deepCls.setFunctions(fbSet);
//            return deepCls;
//          });
//      });
//  }
//
//
//
//
//  public Optional<OaasFunctionBinding> findFunction(OaasClass cls, String funcName) {
//    return cls.getFunctions()
//      .stream()
//      .filter(fb -> funcName.equals(fb.getName()) || funcName.equals(fb.getFunction()))
//      .findFirst();
//  }

}
