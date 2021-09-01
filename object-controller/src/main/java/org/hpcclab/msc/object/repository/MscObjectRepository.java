package org.hpcclab.msc.object.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import io.vertx.core.impl.NoStackTraceThrowable;
import org.hpcclab.msc.object.entity.MscFunction;
import org.hpcclab.msc.object.entity.object.FileState;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.entity.object.MscObjectOrigin;
import org.hpcclab.msc.object.model.RootMscObjectCreating;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
public class MscObjectRepository implements ReactivePanacheMongoRepository<MscObject> {

  public Uni<MscObject> createRootAndPersist(MscObject object) {
    object.setOrigin(null);
    object.setId(null);
    object.removeIgnored();
    return this.persist(object);
  }

}
