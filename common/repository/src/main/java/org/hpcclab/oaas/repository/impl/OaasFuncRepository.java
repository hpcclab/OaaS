package org.hpcclab.oaas.repository.impl;

import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.proto.OaasSchema;
import org.hpcclab.oaas.repository.mapper.ModelMapper;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.infinispan.client.hotrod.RemoteCache;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class OaasFuncRepository extends AbstractIfnpRepository<String, OaasFunction> {

  private static final String NAME = OaasSchema.makeFullName(OaasFunction.class);
  @Inject
  @Remote("OaasFunction")
  RemoteCache<String, OaasFunction> cache;

  @PostConstruct
  void setup() {
    setRemoteCache(cache);
  }

  @Override
  public String getEntityName() {
    return NAME;
  }


  @Override
  protected String extractKey(OaasFunction oaasFunction) {
    return oaasFunction.getName();
  }
}
