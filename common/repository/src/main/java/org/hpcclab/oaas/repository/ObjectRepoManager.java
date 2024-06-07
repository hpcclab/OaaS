package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.model.object.POObject;

import java.util.Collection;
import java.util.List;

public abstract class ObjectRepoManager extends RepoManager<OClass, POObject, ObjectRepository> {

  public Uni<POObject> persistAsync(POObject newObj) {
    return getOrCreate(newObj.getMeta().getCls())
      .async()
      .persistAsync(newObj);
  }

  public Uni<Void> persistAsync(Collection<POObject> newObjs) {
    return Multi.createFrom().iterable(newObjs)
      .onItem()
      .transformToUniAndMerge(obj -> getOrCreate(obj.getMeta().getCls())
        .async()
        .persistAsync(obj))
      .collect()
      .last()
      .replaceWithVoid();
  }

  public Uni<Void> persistWithRevAsync(List<POObject> oldObjs) {
    return Multi.createFrom().iterable(oldObjs)
      .onItem()
      .transformToUniAndMerge(obj -> getOrCreate(obj.getMeta().getCls())
        .atomic()
        .persistWithRevAsync(obj))
      .collect()
      .last()
      .replaceWithVoid();
  }
}
