package org.hpcclab.oaas.repository;

import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.function.OaasFunctionType;
import org.hpcclab.oaas.model.object.ObjectOrigin;
import org.hpcclab.oaas.model.object.ObjectType;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.mapper.ModelMapper;
import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hpcclab.oaas.model.proto.OaasSchema.makeFullName;

@ApplicationScoped
public class OaasObjectRepository extends AbstractIfnpRepository<String, OaasObject> {
  static final String NAME = makeFullName(OaasObject.class);

  private static final Logger LOGGER = LoggerFactory.getLogger(OaasObjectRepository.class);

  @Inject
  OaasClassRepository classRepo;
  @Inject
  @Remote("OaasObject")
  RemoteCache<String, OaasObject> cache;
  @Inject
  ModelMapper oaasMapper;

  @PostConstruct
  void setup() {
    setRemoteCache(cache);
  }

  @Override
  public String getEntityName() {
    return NAME;
  }

  public String generateId() {
    return UUID.randomUUID().toString();
  }

  public Uni<OaasObject> createRootAndPersist(OaasObject object) {
    var cls = classRepo.get(object.getCls());
    if (cls==null) {
      throw NoStackException.notFoundCls400(object.getCls());
    }

    object.setId(generateId());
    object.setOrigin(new ObjectOrigin().setRootId(object.getId()));

    if (cls.getObjectType()==ObjectType.COMPOUND) {
      object.setState(null);
      // TODO check members
    } else {
      object.setRefs(null);
    }
    return this.putAsync(object.getId(), object);
  }

  public Uni<List<OaasObject>> listByIdsAsync(List<String> ids) {
    if (ids==null || ids.isEmpty()) return Uni.createFrom().item(List.of());
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

  public List<OaasObject> listByIds(List<String> ids) {
    if (ids==null || ids.isEmpty()) return List.of();
    var map = remoteCache.getAll(Set.copyOf(ids));
    return ids.stream()
      .map(id -> {
        var obj = map.get(id);
        if (obj==null) throw NoStackException.notFoundObject400(id);
        return obj;
      })
      .toList();
  }

  public Pagination<OaasObject> listByCls(String clsName,
                                          long offset,
                                          int limit) {
    if (clsName==null) return pagination(offset, limit);
    var query = "FROM %s WHERE cls=:clsName".formatted(getEntityName());

    return query(query, Map.of("clsName", clsName), offset, limit);
  }

//  public Uni<DeepOaasObject> getDeep(String id) {
//    return getAsync(id)
//      .onItem().ifNull().failWith(() -> NoStackException.notFoundObject400(id))
//      .flatMap(obj -> {
//        var deep = oaasMapper.deep(obj);
//        return classRepo.getDeep(obj.getCls())
//          .map(deep::setCls);
//      });
//  }

  public OaasObject persist(OaasObject o) {
    if (o==null)
      throw new NoStackException("Cannot persist null object");

    if (o.getId()==null) o.setId(generateId());
    return put(o.getId(), o);
  }

  public Uni<OaasObject> persistAsync(OaasObject o) {
    if (o==null)
      return Uni.createFrom().failure(() -> new NoStackException("Cannot persist null object"));

    if (o.getId()==null) o.setId(generateId());
    return this.putAsync(o.getId(), o);
  }

  public Uni<Void> persistAsync(Collection<OaasObject> objects) {
    var map = objects.stream()
      .collect(Collectors.toMap(OaasObject::getId, Function.identity()));
    return this.putAllAsync(map);
  }

  public Uni<FunctionExecContext> persistFromCtx(FunctionExecContext context) {
    if (context.getFunction().getType()==OaasFunctionType.MACRO) {
      var list = new ArrayList<>(context.getTaskOutputs());
      list.add(context.getOutput());
      return persistAsync(list)
        .replaceWith(context);
    } else {
      return persistAsync(context.getOutput())
        .replaceWith(context);
    }
  }
}
