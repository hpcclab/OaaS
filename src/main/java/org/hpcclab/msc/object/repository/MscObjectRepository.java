package org.hpcclab.msc.object.repository;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import org.bson.types.ObjectId;
import org.hpcclab.msc.object.entity.MscObject;
import org.hpcclab.msc.object.entity.MscObjectOrigin;
import org.hpcclab.msc.object.entity.MscObjectState;
import org.hpcclab.msc.object.model.RootMscObjectCreating;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
public class MscObjectRepository implements ReactivePanacheMongoRepository<MscObject> {

  public Uni<MscObject> createRootAndPersist(RootMscObjectCreating creating) {
    MscObject object = new MscObject();
    object.setType(creating.getType())
      .setState(new MscObjectState().setMainFileUrl(creating.getSourceUrl()))
      .setFunctions(creating.getFunctions())
      .setOrigin(new MscObjectOrigin().setRoot(true));
    return this.persist(object);
  }

  public Uni<MscObject> lazyFuncCall(ObjectId oid,
                                     String funcName,
                                     Map<String, String> args) {
//    findById(oid)

    return null;
  }
}
