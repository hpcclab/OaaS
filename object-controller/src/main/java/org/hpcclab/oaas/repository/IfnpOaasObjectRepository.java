package org.hpcclab.oaas.repository;

import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.mapper.OaasMapper;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.object.DeepOaasObject;
import org.hpcclab.oaas.model.object.OaasObjectOrigin;
import org.hpcclab.oaas.model.object.OaasObjectType;
import org.hpcclab.oaas.model.proto.OaasObject;
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
public class IfnpOaasObjectRepository extends AbstractIfnpRepository<UUID, OaasObject> {
  static final String NAME = OaasObject.class.getName();
  private static final Logger LOGGER = LoggerFactory.getLogger(IfnpOaasObjectRepository.class);

  @Inject
  IfnpOaasClassRepository classRepo;
  @Inject
  @Remote("OaasObject")
  RemoteCache<UUID, OaasObject> cache;
  @Inject
  OaasMapper oaasMapper;

  @PostConstruct
  void setup() {
    setRemoteCache(cache);
  }

  @Override
  public String getEntityName() {
    return NAME;
  }

  public Uni<OaasObject> createRootAndPersist(OaasObject object) {
    var cls = classRepo.get(object.getCls());
    if (cls==null) {
      throw NoStackException.notFoundCls400(object.getCls());
    }
    object.setOrigin(null);
    object.setId(UUID.randomUUID());
    if (object.getOrigin()==null) object.setOrigin(
      new OaasObjectOrigin().setRootId(object.getId()));

    if (cls.getObjectType()==OaasObjectType.COMPOUND) {
      object.setState(null);
      // TODO check members
    } else {
      object.setMembers(null);
    }
    return this.putAsync(object.getId(), object);
  }

  public Uni<List<OaasObject>> listByIdsAsync(List<UUID> ids) {
    if (ids == null || ids.isEmpty()) return Uni.createFrom().item(List.of());
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

  public List<OaasObject> listByIds(List<UUID> ids) {
    if (ids ==null || ids.isEmpty()) return List.of();
    var map =  remoteCache.getAll(Set.copyOf(ids));
    return ids.stream()
      .map(id -> {
        var obj = map.get(id);
        if (obj==null) throw NoStackException.notFoundObject400(id);
        return obj;
      })
      .toList();
  }

  public Uni<DeepOaasObject> getDeep(UUID id) {
    System.out.println("get deep " + id);
    return getAsync(id)
      .onItem().ifNull().failWith(() -> NoStackException.notFoundObject400(id))
      .flatMap(obj -> {
        var deep = oaasMapper.deep(obj);
        return classRepo.getDeep(obj.getCls())
          .map(deep::setCls);
      });
  }

  public OaasObject persist(OaasObject o) {
    if (o == null)
      throw  new NoStackException("Cannot persist null object");

    if (o.getId() == null) o.setId(UUID.randomUUID());
    return put(o.getId(), o);
  }

  public Uni<OaasObject> persistAsync(OaasObject o) {
    if (o == null)
      return Uni.createFrom().failure( ()-> new NoStackException("Cannot persist null object"));

    if (o.getId() == null) o.setId(UUID.randomUUID());
    return this.putAsync(o.getId(), o);
  }
}
