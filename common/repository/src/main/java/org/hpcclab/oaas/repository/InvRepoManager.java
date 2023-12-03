package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.invocation.InvocationNode;

import java.util.Collection;

public abstract class InvRepoManager extends RepoManager<OClass, InvocationNode, InvNodeRepository> {

  public Uni<InvocationNode> persistAsync(InvocationNode newObj) {
    return getOrCreate(newObj.getCls())
      .async()
      .persistAsync(newObj);
  }

  public Uni<Void> persistAsync(Collection<InvocationNode> newObjs) {
    return Multi.createFrom().iterable(newObjs)
      .onItem()
      .transformToUniAndMerge(obj -> getOrCreate(obj.getCls())
        .async()
        .persistAsync(obj))
      .collect()
      .last()
      .replaceWithVoid();
  }
}
