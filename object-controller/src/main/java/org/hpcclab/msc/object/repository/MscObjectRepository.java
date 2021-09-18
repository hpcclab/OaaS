package org.hpcclab.msc.object.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import org.bson.types.ObjectId;
import org.hpcclab.msc.object.entity.object.MscObject;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collection;
import java.util.List;

@ApplicationScoped
public class MscObjectRepository implements ReactivePanacheMongoRepository<MscObject> {

  public Uni<MscObject> createRootAndPersist(MscObject object) {
    object.setOrigin(null);
    object.setId(null);
    object.format();
    return this.persist(object);
  }

  public Uni<List<MscObject>> listByIds(Collection<ObjectId> ids) {
    return find("_id in ?1", ids).list();
  }

  @Override
  public Uni<MscObject> persist(MscObject mscObject) {
    mscObject.updateHash();
    return ReactivePanacheMongoRepository.super.persist(mscObject);
  }

  @Override
  public Uni<MscObject> persistOrUpdate(MscObject mscObject) {
    mscObject.updateHash();
    return ReactivePanacheMongoRepository.super.persistOrUpdate(mscObject);
  }

  @Override
  public Uni<MscObject> update(MscObject mscObject) {
    mscObject.updateHash();
    return ReactivePanacheMongoRepository.super.update(mscObject);
  }
}
