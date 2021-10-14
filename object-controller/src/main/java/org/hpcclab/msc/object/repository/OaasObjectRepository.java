package org.hpcclab.msc.object.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.object.OaasObject;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OaasObjectRepository implements PanacheRepositoryBase<OaasObject, UUID> {

  public Uni<OaasObject> createRootAndPersist(OaasObject object) {
    object.setOrigin(null);
    object.setId(null);
    object.format();
    return this.persist(object);
  }

  public Uni<List<OaasObject>> listByIds(Collection<UUID> ids) {
    return find("_id in ?1", ids).list();
  }
}
