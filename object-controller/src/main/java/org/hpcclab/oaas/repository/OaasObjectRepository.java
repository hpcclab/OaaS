package org.hpcclab.oaas.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hpcclab.oaas.entity.function.OaasFunction;
import org.hpcclab.oaas.entity.function.OaasFunctionBinding;
import org.hpcclab.oaas.entity.object.OaasCompoundMember;
import org.hpcclab.oaas.entity.object.OaasObject;
import org.hpcclab.oaas.exception.NoStackException;
import org.hpcclab.oaas.exception.ObjectValidationException;
import org.hpcclab.oaas.mapper.OaasMapper;
import org.hpcclab.oaas.model.OaasFunctionBindingDto;
import org.hpcclab.oaas.model.OaasObjectDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityGraph;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class OaasObjectRepository implements PanacheRepositoryBase<OaasObject, UUID> {
  private static final Logger LOGGER = LoggerFactory.getLogger(OaasObjectRepository.class);
  @Inject
  OaasMapper oaasMapper;
  @Inject
  OaasClassRepository classRepo;


  public Uni<OaasObject> createRootAndPersist(OaasObjectDto objectDto) {
    var root = oaasMapper.toObject(objectDto);
    root.setOrigin(null);
    root.setId(null);
    root.format();
    return getSession().flatMap(session -> {
      if (root.getType()==OaasObject.ObjectType.COMPOUND) {
        var newMembers = objectDto.getMembers().stream()
          .map(member -> {
            return new OaasCompoundMember().setName(member.getName())
              .setObject(session.getReference(OaasObject.class, member.getObject()));
          })
          .collect(Collectors.toSet());
        root.setMembers(newMembers);
      }

      var funcs = objectDto.getFunctions()
        .stream()
        .map(fbDto ->  new OaasFunctionBinding()
              .setAccess(fbDto.getAccess())
              .setFunction(session.getReference(OaasFunction.class, fbDto.getFunction()))
        )
        .collect(Collectors.toSet());
      root.setFunctions(funcs);
      return this.persist(root);
    });
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
          .map(id -> {
            if (!map.containsKey(id))
              throw new NoStackException("Not found object(id='" + id + "')", 400);
            else
              return map.get(id);
          })
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
      return findWithGraph(id, session, objGraph);
    });
  }

  public Uni<OaasObject> getDeepWithMembers(UUID id) {
    return getSession().flatMap(session -> {
      var objGraph = session.getEntityGraph(OaasObject.class, "oaas.object.deep");
      return session.find(objGraph, id)
        .onItem().ifNull().failWith(() -> new NoStackException("Not found object(id='" + id + "')", 404))
        .invoke(session::detach);
//        .flatMap(obj -> {
//            if (obj.getCls()==null) {
//              return Uni.createFrom().item(obj);
//            }
//            session.detach(obj.getCls());
//            return classRepo.getDeep(obj.getCls().getName())
//              .map(obj::setCls);
//          }
//        )
//        .flatMap(obj -> {
//          if (obj.getType()!=OaasObject.ObjectType.COMPOUND) {
//            return Uni.createFrom().item(obj);
//          }
//          return Multi.createFrom().iterable(obj.getMembers())
//            .call(member -> {
//              session.detach(member.getObject());
//              return getDeepWithMembers(member.getObject().getId())
//                .invoke(member::setObject);
//            })
//            .collect().last()
//            .map(ignore -> obj);
//        });
    });
  }

  public Uni<OaasObject> refreshWithDeep(OaasObject oaasObject) {
    return getSession().flatMap(session -> {
      var objGraph = session.getEntityGraph(OaasObject.class, "oaas.object.deep");
      var id = oaasObject.getId();
      session.detach(oaasObject);
      return findWithGraph(id, session, objGraph);
    });
  }

  private Uni<? extends OaasObject> findWithGraph(UUID id,
                                                  Mutiny.Session session,
                                                  EntityGraph<OaasObject> objGraph) {
    return session.find(objGraph, id)
      .onItem().ifNull()
      .failWith(() -> new NoStackException("Not found object(id='" + id + "')", 404))
      .flatMap(obj -> {
          if (obj.getCls()==null) {
            return Uni.createFrom().item(obj);
          }
          session.detach(obj.getCls());
          return classRepo.getDeep(obj.getCls().getName())
            .map(obj::setCls);
        }
      );
  }


  public Uni<OaasObject> getById(UUID id) {
    return find(
      """
        select o
        from OaasObject o
        left join fetch o.functions
        left join fetch o.members
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
        left join fetch o.members
        """)
      .list();
  }
}
