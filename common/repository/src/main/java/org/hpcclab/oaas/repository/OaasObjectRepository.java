package org.hpcclab.oaas.repository;

import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.repository.mapper.ModelMapper;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class OaasObjectRepository extends AbstractIfnpRepository<String, OaasObject> {
  static final String NAME = OaasObject.class.getName();
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
    object.setOrigin(new OaasObjectOrigin().setRootId(object.getId()));

    if (cls.getObjectType()==OaasObjectType.COMPOUND) {
      object.setState(null);
      // TODO check members
    } else {
      object.setRefs(null);
    }
    return this.putAsync(object.getId(), object);
  }

  public Uni<List<OaasObject>> listByIdsAsync(List<String> ids) {
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

  public List<OaasObject> listByIds(List<String> ids) {
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

  public Pagination<OaasObject> listByCls(String clsName,
                                          long offset,
                                          int limit) {
    if (clsName ==null) return pagination(offset,limit);
    var query = "FROM %s WHERE cls=:clsName".formatted(getEntityName());

    return query(query, Map.of("clsName", clsName), offset, limit);
  }

  public Uni<DeepOaasObject> getDeep(String id) {
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

    if (o.getId() == null) o.setId(generateId());
    return put(o.getId(), o);
  }

  public Uni<OaasObject> persistAsync(OaasObject o) {
    if (o == null)
      return Uni.createFrom().failure( ()-> new NoStackException("Cannot persist null object"));

    if (o.getId() == null) o.setId(generateId());
    return this.putAsync(o.getId(), o);
  }

  public Uni<Void> persistAsync(Collection<OaasObject> objects) {
    var map = objects.stream()
      .collect(Collectors.toMap(OaasObject::getId, Function.identity()));
    return this.putAllAsync(map);
  }


  public List<Map<String, OaasObjectOrigin>> getOrigin(String id, Integer deep) {
    List<Map<String, OaasObjectOrigin>> results = new ArrayList<>();
    var main = get(id);
    if (main == null)
      throw NoStackException.notFoundObject400(id);
    results.add( Map.of(id, main.getOrigin()));
    for (int i = 1; i < deep; i++) {
        Set<String> ids = results.get(i - 1).values()
          .stream()
          .filter(o -> o.getParentId()!=null)
          .flatMap(origin -> Stream.concat(Stream.of(origin.getParentId()), origin.getInputs()
            .stream())
          )
          .collect(Collectors.toSet());

        if (ids.isEmpty()) {
          results.add(i, Map.of());
        } else {
          var list = list(ids);
          var map = list.values().stream()
            .map(o -> Map.entry(o.getId().toString(), o.getOrigin()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
          results.add(i, map);
        }
    }
    return results;
  }

    public Uni<List<Map<String, OaasObjectOrigin>>> getOriginAsync(String id, Integer deep) {
    List<Map<String, OaasObjectOrigin>> results = new ArrayList<>();
    return Multi.createFrom().range(0, deep)
      .call(i -> {
        if (i==0) {
          return getAsync(id)
            .onItem().ifNull().failWith(() -> NoStackException.notFoundObject400(id))
            .map(o -> Map.of(id.toString(), o.getOrigin()))
            .invoke(map -> results.add(i, map));
        } else {
          Set<String> ids = results.get(i - 1).values()
            .stream()
            .filter(o -> o.getParentId()!=null)
            .flatMap(origin -> Stream.concat(Stream.of(origin.getParentId()), origin.getInputs()
              .stream())
            )
            .collect(Collectors.toSet());

          if (ids.isEmpty()) {
            results.add(i, Map.of());
            return Uni.createFrom().item(Map.of());
          }

          return listAsync(ids)
            .map(objs -> objs.values().stream()
              .map(o -> Map.entry(o.getId().toString(), o.getOrigin()))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
            )
            .invoke(map -> results.add(i, map));
        }
      })
      .collect().last()
      .map(v -> results);
  }

}
