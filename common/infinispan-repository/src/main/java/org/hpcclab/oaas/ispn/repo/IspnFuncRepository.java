package org.hpcclab.oaas.ispn.repo;

import io.quarkus.infinispan.client.Remote;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.proto.OaasSchema;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.infinispan.client.hotrod.RemoteCache;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class IspnFuncRepository extends AbstractIspnRepository<String, OaasFunction>
implements FunctionRepository {

  private static final String NAME = OaasSchema.makeFullName(OaasFunction.class);
  @Inject
  @Remote("OaasFunction")
  RemoteCache<String, OaasFunction> cache;

  @Override
  public RemoteCache<String, OaasFunction> getRemoteCache() {
    return cache;
  }

  @Override
  public String getEntityName() {
    return NAME;
  }


  @Override
  protected String extractKey(OaasFunction oaasFunction) {
    return oaasFunction.getKey();
  }


}
