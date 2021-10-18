package org.hpcclab.msc.object.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.object.OaasObject;
import org.hpcclab.msc.object.exception.NoStackException;
import org.hpcclab.msc.object.exception.ObjectValidationException;
import org.hpcclab.msc.object.mapper.OaasMapper;
import org.hpcclab.msc.object.model.OaasFunctionBindingDto;
import org.hpcclab.msc.object.model.OaasObjectDto;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OaasObjectRepository implements PanacheRepositoryBase<OaasObject, UUID> {
  @Inject
  OaasMapper oaasMapper;


  public Uni<OaasObject> createRootAndPersist(OaasObjectDto object) {
    var root = oaasMapper.toObject(object);
    root.setOrigin(null);
    root.setId(null);
    root.format();
    return this.persist(root);
  }

  public Uni<List<OaasObject>> listByIds(Collection<UUID> ids) {
    return find("_id in ?1", ids).list();
  }

  public Uni<OaasObject> bindFunction(UUID id, List<OaasFunctionBindingDto> bindingDtoList) {
    return findById(id)
      .onItem().ifNull().failWith(() -> new NoStackException("Not found object with given id", 404))
      .flatMap(object -> {
        verifyBinding(object);
        object.getFunctions().addAll(oaasMapper.toBinding(bindingDtoList));
        return persistAndFlush(object);
      });
  }

  public void verifyBinding(OaasObject object) {
    if (object.getAccess() != OaasObject.AccessModifier.PUBLIC){
      throw new ObjectValidationException("Object is no public");
    }
  }

  public Uni<OaasObject> getTree(UUID id) {
    return getSession().flatMap(session -> {
      var graph=session.getEntityGraph(OaasObject.class, "oaas.object.tree");
      return session.find(graph, id);
    });
  }
}
