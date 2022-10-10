package org.hpcclab.oaas.infinispan;

import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.proto.OaasSchema;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.infinispan.client.hotrod.RemoteCache;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Objects;

@ApplicationScoped
public class InfFuncRepository extends AbstractInfRepository<String, OaasFunction>
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
    return oaasFunction.getName();
  }


}
