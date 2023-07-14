package org.hpcclab.oaas.ispn.repo;

import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.hpcclab.oaas.model.proto.OaasSchema.makeFullName;

@ApplicationScoped
public class IspnObjectRepository extends AbstractIspnRepository<String, OaasObject>
  implements ObjectRepository {
  static final String NAME = makeFullName(OaasObject.class);

  private static final Logger LOGGER = LoggerFactory.getLogger(IspnObjectRepository.class);

  @Inject
  @Remote("OaasObject")
  RemoteCache<String, OaasObject> cache;
  @Inject
  Vertx vertx;

  @Override
  public RemoteCache<String, OaasObject> getRemoteCache() {
    return cache;
  }

  @Override
  public String getEntityName() {
    return NAME;
  }

  @Override
  public Uni<Pagination<OaasObject>> listByCls(List<String> clsKeys, long offset,
                                               int limit) {

    return vertx.executeBlocking(Uni.createFrom().item(() -> {
      if (clsKeys==null || clsKeys.isEmpty())
        return getQueryService().pagination(offset, limit);
      var query = "FROM %s WHERE cls=:clsName".formatted(getEntityName());
      return getQueryService()
        .queryPagination(query, Map.of("clsName", clsKeys), offset, limit);
    }));
  }

  @Override
  public Uni<Pagination<OaasObject>> sortedListByCls(List<String> clsKeys, String sortKey, boolean desc, long offset, int limit) {
    return listByCls(clsKeys, offset, limit);
  }

  @Override
  protected String extractKey(OaasObject object) {
    return object.getId();
  }
}
