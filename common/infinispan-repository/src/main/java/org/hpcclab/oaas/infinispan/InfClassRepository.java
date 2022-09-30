package org.hpcclab.oaas.infinispan;

import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.proto.OaasSchema;
import org.hpcclab.oaas.repository.ClassRepository;
import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;

@ApplicationScoped
public class InfClassRepository extends AbstractInfRepository<String, OaasClass>
implements ClassRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(InfClassRepository.class);
  private static final String NAME = OaasSchema.makeFullName(OaasClass.class);

  @Inject
  @Remote("OaasClass")
  RemoteCache<String, OaasClass> cache;

  @Override
  public RemoteCache<String, OaasClass> getRemoteCache() {
    return cache;
  }

  @Override
  public String getEntityName() {
    return NAME;
  }

  @Override
  protected String extractKey(OaasClass oaasClass) {
    return oaasClass.getName();
  }

  @Override
  public Map<String, OaasClass> resolveInheritance(Map<String, OaasClass> clsMap) {
    throw StdOaasException.notImplemented();
  }

}
