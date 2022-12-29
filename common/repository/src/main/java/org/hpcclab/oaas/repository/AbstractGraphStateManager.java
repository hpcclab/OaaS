package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.model.TaskContext;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
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
  public Multi<OaasObject> handleComplete(OaasTask task, TaskCompletion completion) {
    var main = task.getMain();
    main.update(completion.getMain());
    var out = task.getOutput();
    if (out != null)
      out.updateStatus(completion);
    return persistThenLoadNext(main, out, !completion.isSuccess());
  }

  @Override
  public Multi<OaasObject> handleComplete(TaskContext taskContext, TaskCompletion completion) {
    var main = taskContext.getMain();
    main.update(completion.getMain());
    var out = taskContext.getOutput();
    if (out != null)
      out.updateStatus(completion);
    return persistThenLoadNext(main, out, !completion.isSuccess());
  }

  private Multi<OaasObject> persistThenLoadNext(OaasObject main,
                                                OaasObject out,
                                                boolean isFail) {
    var objs = out == null? List.of(main): List.of(main,out);
    Uni<Void> uni =  objRepo
      .persistWithPreconditionAsync(objs);
    if (out == null)
      return uni.onItem().transformToMulti(__ -> Multi.createFrom().empty());
    return uni.onItem()
      .transformToMulti(__ -> {
        if (!isFail) {
          return loadNextSubmittable(out);
        } else {
          return handleFailed(out);
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


  public Multi<OaasObject> handleFailed(OaasObject failedObj) {
    return getDependentsRecursive(failedObj.getId())
      .onItem()
      .transformToUniAndConcatenate(id -> objRepo.computeAsync(id, (k, v) -> v.markAsFailed()))
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
    var originator = entryCtx.getOutput() != null?
      entryCtx.getOutput().getId()
      : entryCtx.getMain().getId();
    return Multi.createFrom().iterable(contexts)
      .onItem().transformToUniAndConcatenate(ctx -> {
        if (ctx.getOutput() == null){
          return Uni.createFrom().item(ctx);
        }
        if (entryCtx.contains(ctx)) {
          ctx.getOutput().markAsSubmitted(originator, true);
          return Uni.createFrom().item(ctx);
        } else {
          return objRepo.computeAsync(ctx.getOutput().getId(), (id, obj) -> obj.markAsSubmitted(originator, true))
            .map(ctx::setOutput);
        }
      })
      .filter(ctx -> ctx.getOutput() == null || ctx.getOutput().getStatus().getOriginator().equals(originator))
      .onCompletion().call(() -> persistAllWithoutNoti(entryCtx));
  }

  public Uni<?> persistAllWithoutNoti(FunctionExecContext ctx) {
    return persistAllWithoutNoti(ctx, Lists.mutable.empty());
  }
  public Uni<?> persistAllWithoutNoti(FunctionExecContext ctx, List<OaasObject> objs) {
    objs.addAll(ctx.getSubOutputs());
    var dataflow = ctx.getFunction().getMacro();
    if (ctx.getOutput() != null && (dataflow==null || dataflow.getExport()==null)) {
      objs.add(ctx.getOutput());
    }

    return objRepo.persistAsync(objs, false);
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
      .setSmtTs(System.currentTimeMillis())
      .setOriginator(originator);
    return object;
  }
}
