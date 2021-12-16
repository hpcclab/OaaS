package org.hpcclab.oaas.repository;

import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.mapper.OaasMapper;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.object.DeepOaasObjectDto;
import org.hpcclab.oaas.model.object.OaasObjectOrigin;
import org.hpcclab.oaas.model.object.OaasObjectType;
import org.hpcclab.oaas.model.proto.OaasClassPb;
import org.hpcclab.oaas.model.proto.OaasFunctionPb;
import org.hpcclab.oaas.model.proto.OaasObjectPb;
import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class IfnpOaasObjectRepository extends AbstractIfnpRepository<UUID, OaasObjectPb> {
  static final String NAME = OaasObjectPb.class.getName();
  private static final Logger LOGGER = LoggerFactory.getLogger(IfnpOaasObjectRepository.class);

  @Inject
  IfnpOaasClassRepository classRepo;
  @Inject
  @Remote("OaasObject")
  RemoteCache<UUID, OaasObjectPb> cache;

  @PostConstruct
  void setup() {
    setRemoteCache(cache);
  }

  @Override
  public String getEntityName() {
    return NAME;
  }

  public Uni<OaasObjectPb> createRootAndPersist(OaasObjectPb object) {
    var cls = classRepo.get(object.getCls());
    if (cls==null) {
      throw NoStackException.notFoundCls400(object.getCls());
    }
    object.setOrigin(null);
    object.setId(null);
    if (object.getOrigin()==null) object.setOrigin(
      new OaasObjectOrigin().setRootId(object.getId()));

    if (cls.getObjectType()==OaasObjectType.COMPOUND) {
      object.setState(null);
      // TODO check members
    } else {
      object.setMembers(null);
    }
    return this.put(object.getId(), object);
  }

  public Uni<List<OaasObjectPb>> listByIds(List<UUID> ids) {
    if (ids.isEmpty()) return Uni.createFrom().item(List.of());
    return this.listAsync(Set.copyOf(ids))
      .map(map -> ids.stream()
        .map(id -> {
          var obj = map.get(id);
          if (obj==null) throw NoStackException.notFoundObject400(id);
          return obj;
        })
        .toList()
      );
  }

  public Uni<DeepOaasObjectDto> getDeep(UUID id) {
    //TODO
    return null;
  }

  public Uni<OaasObjectPb> persist(OaasObjectPb o) {
    return this.put(o.getId(), o);
  }
}
