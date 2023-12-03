package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.object.OObject;

import java.util.Collection;
import java.util.List;

public abstract class ObjectRepoManager extends RepoManager<OClass, OObject, ObjectRepository> {

  public Uni<OObject> persistAsync(OObject newObj) {
    return getOrCreate(newObj.getCls())
      .async()
      .persistAsync(newObj);
  }

  public Uni<Void> persistAsync(Collection<OObject> newObjs) {
    return Multi.createFrom().iterable(newObjs)
      .onItem()
      .transformToUniAndMerge(obj -> getOrCreate(obj.getCls())
        .async()
        .persistAsync(obj))
      .collect()
      .last()
      .replaceWithVoid();
  }

  public Uni<Void> persistWithRevAsync(List<OObject> oldObjs) {
    return Multi.createFrom().iterable(oldObjs)
      .onItem()
      .transformToUniAndMerge(obj -> getOrCreate(obj.getCls())
        .atomic()
        .persistWithRevAsync(obj))
      .collect()
      .last()
      .replaceWithVoid();
  }
}
