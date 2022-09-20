package org.hpcclab.oaas.infinispan;

import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.object.ObjectOrigin;
import org.hpcclab.oaas.model.object.ObjectType;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;

import static org.hpcclab.oaas.model.proto.OaasSchema.makeFullName;

@ApplicationScoped
public class InfObjectRepository extends AbstractInfRepository<String, OaasObject>
  implements ObjectRepository {
  static final String NAME = makeFullName(OaasObject.class);

  private static final Logger LOGGER = LoggerFactory.getLogger(InfObjectRepository.class);

  @Inject
  InfClassRepository classRepo;
  @Inject
  @Remote("OaasObject")
  RemoteCache<String, OaasObject> cache;

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

  public Pagination<OaasObject> listByCls(String clsName,
                                          long offset,
                                          int limit) {
    if (clsName==null) return pagination(offset, limit);
    var query = "FROM %s WHERE cls=:clsName".formatted(getEntityName());

    return query(query, Map.of("clsName", clsName), offset, limit);
  }


  @Override
  protected String extractKey(OaasObject object) {
    return object.getId();
  }
}
