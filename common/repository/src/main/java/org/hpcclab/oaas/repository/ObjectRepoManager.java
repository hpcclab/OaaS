package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.object.OaasObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ObjectRepoManager {
  protected Map<String, ObjectRepository> repoMap = new HashMap<>();

  public abstract ObjectRepository createRepo(OaasClass cls);
  protected abstract OaasClass load(String clsKey);

  public ObjectRepository getOrCreate(String clsKey) {
    return repoMap.computeIfAbsent(clsKey, __ -> createRepo(load(clsKey)));
  }
  public ObjectRepository getOrCreate(OaasClass cls) {
    return repoMap.computeIfAbsent(cls.getKey(), k -> createRepo(cls));
  }


  public Uni<OaasObject> persistAsync(OaasObject newObj){
    return getOrCreate(newObj.getCls())
      .async()
      .persistAsync(newObj);
  }
  public Uni<Void> persistAsync(Collection<OaasObject> newObjs){
    return Multi.createFrom().iterable(newObjs)
      .onItem()
      .transformToUniAndMerge(obj -> getOrCreate(obj.getCls())
        .async()
        .persistAsync(obj))
      .collect()
      .last()
      .replaceWithVoid();
  }

  public Uni<Void> persistWithRevAsync(List<OaasObject> oldObjs){
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
