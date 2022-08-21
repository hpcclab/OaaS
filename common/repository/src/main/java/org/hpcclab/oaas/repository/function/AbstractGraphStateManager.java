package org.hpcclab.oaas.repository.function;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.model.TaskContext;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.hpcclab.oaas.repository.EntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;

public abstract class AbstractGraphStateManager implements GraphStateManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGraphStateManager.class);
  protected EntityRepository<String, OaasObject> objRepo;


  protected AbstractGraphStateManager() {
  }

  protected AbstractGraphStateManager(EntityRepository<String, OaasObject> objRepo) {
    this.objRepo = objRepo;
  }

  @Override
  public Multi<OaasObject> handleComplete(TaskCompletion completion) {
    return objRepo.computeAsync(completion.getId(), (k, obj) -> updateCompletedObject(obj, completion))
      .onItem()
      .transformToMulti(completingObj -> {
        if (!completingObj.getStatus().getTaskStatus().isFailed()) {
          return loadNextSubmittable(completingObj);
        } else {
          return handleFailed(completingObj);
        }
      });
  }

  private Multi<OaasObject> loadNextSubmittable(OaasObject completedObject) {
    return getAllEdge(completedObject.getId())
      .toMulti()
      .flatMap(l -> Multi.createFrom().iterable(l))
      .onItem()
      .transformToUniAndConcatenate(id -> objRepo.computeAsync(id,
        (k, obj) -> triggerObject(obj, completedObject.getStatus().getOriginator(), completedObject.getId())))
      .filter(obj -> obj.getStatus().getOriginator().equals(completedObject.getStatus().getOriginator()));
  }

  private OaasObject updateCompletedObject(OaasObject obj, TaskCompletion completion) {
    if (obj==null) throw NoStackException.notFoundObject400(completion.getId());
    if (obj.getStatus().getSubmittedTime() <= 0) {
      LOGGER.warn("completing object {} has no submittedTime", obj.getId());
    }
    obj.updateStatus(completion);
    return obj;
  }

  public Multi<OaasObject> handleFailed(OaasObject failedObj) {
    return getDependentsRecursive(failedObj.getId())
      .onItem()
      .transformToUniAndConcatenate(id -> objRepo.computeAsync(id, (k, v) -> updateFailingObject(v)))
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
    var originator = entryCtx.getOutput().getId();
    return Multi.createFrom().iterable(contexts)
      .onItem().transformToUniAndConcatenate(ctx -> {
        if (entryCtx.contains(ctx)) {
          updateSubmittingObject(ctx.getOutput(), originator);
          return Uni.createFrom().item(ctx);
        } else {
          return objRepo.computeAsync(ctx.getOutput().getId(), (id, obj) -> updateSubmittingObject(obj, originator))
            .map(ctx::setOutput);
        }
      })
      .filter(ctx -> {
        var status = ctx.getOutput().getStatus();
        if (status.getSubmittedTime() <= 0) {
          LOGGER.warn("Detect object {} without SubmittedTime [originator={}, ctxId={}]",
            ctx.getOutput().getId(),
            status.getOriginator(),
            entryCtx.getOutput().getId());
        }
        return ctx.getOutput().getStatus().getOriginator().equals(originator);
      })
      .onCompletion().call(() -> persistAllWithoutNoti(entryCtx));
  }

  private Uni<?> persistAllWithoutNoti(FunctionExecContext ctx) {
    var objs = Lists.mutable.ofAll(ctx.getSubOutputs());
    var dataflow = ctx.getFunction().getMacro();
    if (dataflow==null || dataflow.getExport()==null) {
      objs.add(ctx.getOutput());
    }
    return objRepo.persistAsync(objs, false);
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
    object.setStatus(status);
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
    var list = Lists.mutable.ofAll(status.getWaitFor());
    list.remove(srcId);
    status.setWaitFor(list);
    if (ts.isSubmitted() || ts.isFailed() || !status.getWaitFor().isEmpty())
      return object;
    status
      .setTaskStatus(TaskStatus.DOING)
      .setSubmittedTime(System.currentTimeMillis())
      .setOriginator(originator);
    return object;
  }
}
