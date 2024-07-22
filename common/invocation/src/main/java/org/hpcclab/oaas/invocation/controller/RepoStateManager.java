package org.hpcclab.oaas.invocation.controller;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.state.DeleteStateOperation;
import org.hpcclab.oaas.invocation.state.SimpleStateOperation;
import org.hpcclab.oaas.invocation.state.StateManager;
import org.hpcclab.oaas.model.object.GOObject;
import org.hpcclab.oaas.repository.ObjectRepoManager;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author Pawissanutt
 */
public class RepoStateManager implements StateManager {
  final ObjectRepoManager repoManager;

  public RepoStateManager(ObjectRepoManager repoManager) {
    this.repoManager = repoManager;
  }

  @Override
  public Uni<Void> applySimple(SimpleStateOperation operation) {
    Uni<Void> uni;
    if (operation.updateCls()!=null && !operation.updateObjs().isEmpty()) {
      var repo = repoManager.getOrCreate(operation.updateCls());
      uni = repo.atomic().persistWithRevAsync(operation.updateObjs());
    } else {
      uni = Uni.createFrom().voidItem();
    }
    if (operation.createCls()!=null && !operation.createObjs().isEmpty()) {
      var repo = repoManager.getOrCreate(operation.createCls());
      uni = uni.flatMap(v -> repo.async().persistAsync(operation.createObjs()));
    }
    if (uni==null) return Uni.createFrom().voidItem();
    return uni;
  }

  @Override
  public Uni<Void> applyDelete(DeleteStateOperation operation) {
    ObjectRepository repo = repoManager.getOrCreate(operation.cls());
    return Multi.createFrom().iterable(operation.objs())
      .map(GOObject::getKey)
      .filter(Objects::nonNull)
      .call(k -> {
        logger.debug("delete {} {}", operation.cls().getKey(), k);
        return repo.async().deleteAsync(k);
      })
      .collect().last()
      .replaceWithVoid();
  }
  private static final Logger logger = LoggerFactory.getLogger( RepoStateManager.class );
}
