package org.hpcclab.msc.object.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import org.bson.types.ObjectId;
import org.hpcclab.msc.object.entity.object.OaasObject;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collection;
import java.util.List;

@ApplicationScoped
public class MscObjectRepository implements ReactivePanacheMongoRepository<OaasObject> {

  public Uni<OaasObject> createRootAndPersist(OaasObject object) {
    object.setOrigin(null);
    object.setId(null);
    object.format();
    return this.persist(object);
  }

  public Uni<List<OaasObject>> listByIds(Collection<ObjectId> ids) {
    return find("_id in ?1", ids).list();
  }

  @Override
  public Uni<OaasObject> persist(OaasObject mscObject) {
    mscObject.updateHash();
    return ReactivePanacheMongoRepository.super.persist(mscObject);
  }

  @Override
  public Uni<OaasObject> persistOrUpdate(OaasObject mscObject) {
    mscObject.updateHash();
    return ReactivePanacheMongoRepository.super.persistOrUpdate(mscObject);
  }

  @Override
  public Uni<OaasObject> update(OaasObject mscObject) {
    mscObject.updateHash();
    return ReactivePanacheMongoRepository.super.update(mscObject);
  }
}
