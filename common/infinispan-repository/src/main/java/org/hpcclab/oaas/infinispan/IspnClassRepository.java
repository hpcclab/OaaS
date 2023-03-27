package org.hpcclab.oaas.infinispan;

import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.proto.OaasSchema;
import org.hpcclab.oaas.repository.ClassRepository;
import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class IspnClassRepository extends AbstractIspnRepository<String, OaasClass>
implements ClassRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(IspnClassRepository.class);
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

  @Override
  public Uni<List<String>> listSubCls(String clsKey) {
    throw StdOaasException.notImplemented();
  }
}
