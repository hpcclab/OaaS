package org.hpcclab.msc.object.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.object.OaasObject;
import org.hpcclab.msc.object.mapper.OaasMapper;
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
}
