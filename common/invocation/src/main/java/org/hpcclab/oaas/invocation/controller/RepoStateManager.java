package org.hpcclab.oaas.invocation.controller;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.hpcclab.oaas.repository.ObjectRepoManager;

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
    if (operation.getUpdateCls() != null && !operation.getUpdateObjs().isEmpty()) {
      var repo = repoManager.getOrCreate(operation.getUpdateCls());
      uni = repo.atomic().persistWithRevAsync(operation.getUpdateObjs());
    } else {
      uni = Uni.createFrom().voidItem();
    }
    if (operation.getCreateCls() != null && !operation.getCreateObjs().isEmpty()) {
      var repo = repoManager.getOrCreate(operation.getCreateCls());
      uni = uni.flatMap(v -> repo.async().persistAsync(operation.getCreateObjs()));
    }
    if (uni == null) return Uni.createFrom().voidItem();
    return uni;
  }
}
