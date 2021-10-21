package org.hpcclab.msc.object.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hpcclab.msc.object.entity.OaasClass;
import org.hpcclab.msc.object.entity.object.OaasObject;
import org.hpcclab.msc.object.exception.NoStackException;
import org.hpcclab.msc.object.exception.ObjectValidationException;
import org.hpcclab.msc.object.mapper.OaasMapper;
import org.hpcclab.msc.object.model.OaasFunctionBindingDto;
import org.hpcclab.msc.object.model.OaasObjectDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class OaasObjectRepository implements PanacheRepositoryBase<OaasObject, UUID> {
  private static final Logger LOGGER = LoggerFactory.getLogger( OaasObjectRepository.class );
  @Inject
  OaasMapper oaasMapper;
  @Inject
  OaasClassRepository classRepo;


  public Uni<OaasObject> createRootAndPersist(OaasObjectDto object) {
    var root = oaasMapper.toObject(object);
    root.setOrigin(null);
    root.setId(null);
    root.format();
    return this.persist(root);
  }

  public Uni<List<OaasObject>> listByIds(List<UUID> ids) {
    return find("""
      select o
      from OaasObject o
      where o.id in ?1
      """, ids).list()
      .map(objs -> {
        var map = objs.stream()
          .collect(Collectors.toMap(OaasObject::getId, Function.identity()));
        return ids.stream()
          .map(map::get)
          .toList();
      });
  }

  public Uni<OaasObject> bindFunction(UUID id, List<OaasFunctionBindingDto> bindingDtoList) {
    return getById(id)
      .onItem().ifNull().failWith(() -> new NoStackException("Not found object with given id", 404))
      .flatMap(object -> {
        verifyBinding(object);
        object.getFunctions().addAll(oaasMapper.toBinding(bindingDtoList));
        return persistAndFlush(object);
      });
  }

  public void verifyBinding(OaasObject object) {
    if (object.getAccess()!=OaasObject.AccessModifier.PUBLIC) {
      throw new ObjectValidationException("Object is not public");
    }
  }

  public Uni<OaasObject> getDeep(UUID id) {
    return getSession().flatMap(session -> {
      var objGraph = session.getEntityGraph(OaasObject.class, "oaas.object.deep");
      return session.find(objGraph, id)
        .invoke(session::detach)
        .flatMap(obj -> classRepo.getDeep(obj.getCls().getName())
          .map(obj::setCls)
        );
    });
  }


  public Uni<OaasObject> getDeep2(UUID id) {
    return find("""
    select o
    from OaasObject o
    left join fetch o.functions as fb
    left join fetch o.cls.functions
    where o.id = ?1
    """, id).singleResult();
  }

  public Uni<OaasObject> getById(UUID id) {
    return find(
      """
        select o
        from OaasObject o
        left join fetch o.functions
        where o.id = ?1
        """, id)
      .singleResult();
  }

  public Uni<List<OaasObject>> list() {
    return find(
      """
        select o
        from OaasObject o
        left join fetch o.functions
        """)
      .list();
  }

  public Uni<OaasObject> resolveFunction(UUID id) {
    return getSession().flatMap(session -> getDeep(id)
      .invoke(session::detach)
      .invoke(o -> {
        var fns = new ArrayList<>(o.getCls().getFunctions());
        fns.addAll(o.getFunctions());
        o.setFunctions(fns);
      })
    );
  }
}
