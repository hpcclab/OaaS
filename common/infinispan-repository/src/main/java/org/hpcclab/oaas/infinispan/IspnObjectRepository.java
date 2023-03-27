package org.hpcclab.oaas.infinispan;

import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.object.ObjectOrigin;
import org.hpcclab.oaas.model.object.ObjectType;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hpcclab.oaas.model.proto.OaasSchema.makeFullName;

@ApplicationScoped
public class IspnObjectRepository extends AbstractIspnRepository<String, OaasObject>
  implements ObjectRepository {
  static final String NAME = makeFullName(OaasObject.class);

  private static final Logger LOGGER = LoggerFactory.getLogger(IspnObjectRepository.class);

  @Inject
  IspnClassRepository classRepo;
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

  private String generateId() {
    return UUID.randomUUID().toString();
  }

  public Uni<OaasObject> createRootAndPersist(OaasObject object) {
    var cls = classRepo.get(object.getCls());
    if (cls==null) {
      throw NoStackException.notFoundCls400(object.getCls());
    }

    object.setId(generateId());
    object.setOrigin(new ObjectOrigin());

    if (cls.getObjectType()==ObjectType.COMPOUND) {
      object.setState(null);
      // TODO check members
    } else {
      object.setRefs(null);
    }
    return this.putAsync(object.getId(), object);
  }

  public Uni<Pagination<OaasObject>> listByCls(List<String> clsKeys, long offset,
                                               int limit) {

    return vertx.executeBlocking(Uni.createFrom().item(() -> {
      if (clsKeys==null || clsKeys.isEmpty()) return pagination(offset, limit);
      var query = "FROM %s WHERE cls=:clsName".formatted(getEntityName());
      return queryPagination(query, Map.of("clsName", clsKeys), offset, limit);
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
