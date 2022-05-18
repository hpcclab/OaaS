package org.hpcclab.oaas.repository.function;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.hpcclab.oaas.model.TaskContext;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.hpcclab.oaas.repository.EntityRepository;

import java.util.Collection;
import java.util.Objects;

public abstract class AbstractGraphStateManager implements GraphStateManager {

  protected EntityRepository<String, OaasObject> objRepo;


  public AbstractGraphStateManager() {
  }

  public AbstractGraphStateManager(EntityRepository<String, OaasObject> objRepo) {
    this.objRepo = objRepo;
  }

  @Override
  public Multi<OaasObject> handleComplete(OaasObject completingObj) {
    if (!completingObj.getStatus().getTaskStatus().isFailed()) {
      return getAllEdge(completingObj.getId())
        .toMulti()
        .flatMap(l -> Multi.createFrom().iterable(l))
        .onItem()
        .transformToUniAndConcatenate(id -> objRepo.computeAsync(id,
          (k, obj) -> triggerObject(obj, completingObj.getStatus().getOriginator(), completingObj.getId())))
        .filter(obj -> obj.getStatus().getOriginator().equals(completingObj.getStatus().getOriginator()));
    } else {
      return handleFailed(completingObj);
    }
  }

  public Multi<OaasObject> handleFailed(OaasObject failedObj) {
    return getDependentsRecursive(failedObj.getId())

      .onItem()
      .transformToUniAndConcatenate(id -> objRepo.computeAsync(id, (k, v) -> updateFailingObject(v)))
      .onCompletion()
      .call(() -> objRepo.persistAsync(failedObj))
      .filter(i -> false);

  }

  public Multi<String> getDependentsRecursive(String srcId) {
    return getAllEdge(srcId)
      .onItem().transformToMulti(edges -> {
        if (edges.isEmpty()) {
          return Multi.createFrom().empty();
        }
        return Multi.createFrom().iterable(edges)
          .flatMap(this::getDependentsRecursive)
          .onCompletion().continueWith(edges);
      });
  }

  @Override
  public Multi<TaskContext> updateSubmittingStatus(FunctionExecContext entryCtx, Collection<TaskContext> contexts) {
    return Multi.createFrom().iterable(contexts)
      .onItem().transformToUniAndConcatenate(ctx -> {
        if (entryCtx.contains(ctx)) {
          updateSubmittingObject(ctx.getOutput(), entryCtx.getOutput().getId());
          return Uni.createFrom().item(ctx);
        } else {
          return objRepo.computeAsync(ctx.getOutput().getId(), (id, obj) -> updateSubmittingObject(obj, entryCtx.getOutput().getId()))
            .map(ctx::setOutput);
        }
      })
      .filter(ctx -> ctx.getOutput().getStatus().getOriginator().equals(entryCtx.getOutput().getId()))
      .onCompletion().call(() -> persistAll(entryCtx));
  }

  private Uni<?> persistAll(FunctionExecContext ctx) {
    var objs = Lists.mutable.ofAll(ctx.getSubOutputs());
    var dataflow = ctx.getFunction().getMacro();
    if (dataflow == null || dataflow.getExport() == null) {
      objs.add(ctx.getOutput());
    }
    return objRepo.persistAsync(objs);
  }

  OaasObject updateSubmittingObject(OaasObject object, String originator) {
    var status = object.getStatus();
    var ts = status.getTaskStatus();
    if (ts.isSubmitted() || ts.isFailed())
      return object;
    status
      .setTaskStatus(TaskStatus.DOING)
      .setSubmittedTime(System.currentTimeMillis())
      .setOriginator(originator);
    return object;
  }

  OaasObject updateFailingObject(OaasObject object) {
    var status = object.getStatus();
    var ts = status.getTaskStatus();
    if (ts.isSubmitted() || ts.isFailed())
      return object;
    status
      .setTaskStatus(TaskStatus.DEPENDENCY_FAILED);
    return object;
  }

  OaasObject triggerObject(OaasObject object, String originator, String srcId) {
    Objects.requireNonNull(object);
    var status = object.getStatus();
    var ts = status.getTaskStatus();
    status.getWaitFor().remove(srcId);
    if (ts.isSubmitted() || ts.isFailed() || !status.getWaitFor().isEmpty())
      return object;
    status
      .setTaskStatus(TaskStatus.DOING)
      .setSubmittedTime(System.currentTimeMillis())
      .setOriginator(originator);
    return object;
  }
}
