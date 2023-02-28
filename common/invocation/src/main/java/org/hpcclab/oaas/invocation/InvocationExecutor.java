package org.hpcclab.oaas.invocation;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.hpcclab.oaas.model.exception.DataAccessException;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.model.function.DeploymentCondition;
import org.hpcclab.oaas.model.invocation.InvApplyingContext;
import org.hpcclab.oaas.model.function.FunctionType;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.object.OaasObjects;
import org.hpcclab.oaas.model.task.*;
import org.hpcclab.oaas.repository.GraphStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RegisterForReflection(
  targets = {
    OaasTask.class,
    TaskCompletion.class
  },
  registerFullHierarchy = true
)
public class InvocationExecutor {
  private static final Logger logger = LoggerFactory.getLogger(InvocationExecutor.class);
  InvocationQueueSender sender;
  GraphStateManager gsm;
  ContextLoader contextLoader;
  SyncInvoker syncInvoker;
//  CompletionValidator completionValidator;
  CompletedStateUpdater completionHandler;
  TaskFactory taskFactory;


  @Inject
  public InvocationExecutor(InvocationQueueSender sender,
                            GraphStateManager gsm,
                            ContextLoader contextLoader,
                            SyncInvoker syncInvoker,
                            TaskFactory taskFactory,
//                            CompletionValidator completionValidator,
                            CompletedStateUpdater completionHandler) {
    this.sender = sender;
    this.gsm = gsm;
    this.contextLoader = contextLoader;
    this.syncInvoker = syncInvoker;
    this.taskFactory = taskFactory;
//    this.completionValidator = completionValidator;
    this.completionHandler = completionHandler;
  }


  public boolean canSyncInvoke(InvApplyingContext ctx) {
    var func = ctx.getFunction();
    if (func.getType()==FunctionType.MACRO) {
      return false;
    }
    if (func.getDeploymentStatus().getCondition()!=DeploymentCondition.RUNNING) {
      return false;
    }
    MutableList<Map.Entry<OaasObject, OaasObject>> waitForGraph =
      Lists.mutable.empty();
    MutableList<OaasObject> failDeps = Lists.mutable.empty();
    return ctx.analyzeDeps(waitForGraph, failDeps);
  }

  public Uni<Void> asyncSubmit(InvApplyingContext ctx) {
    Set<TaskContext> ctxToSubmit = Sets.mutable.empty();
    MutableList<Map.Entry<OaasObject, OaasObject>> waitForGraph =
      Lists.mutable.empty();
    MutableList<Map.Entry<OaasObject, OaasObject>> innerWaitForGraph =
      Lists.mutable.empty();
    MutableList<OaasObject> failDeps = Lists.mutable.empty();
    if (ctx.analyzeDeps(waitForGraph, failDeps)) {
      switch (ctx.getFunction().getType()) {
        case MACRO -> {
          for (var subCtx : ctx.getSubContexts()) {
            if (subCtx.getFunction().getType()==FunctionType.TASK
              && subCtx.analyzeDeps(innerWaitForGraph, failDeps))
              ctxToSubmit.add(subCtx);
          }
        }
        case TASK, IM_TASK -> ctxToSubmit.add(ctx);
        default -> {
          // DO NOTHING
        }
      }
    }
    return traverseGraph(waitForGraph, ctxToSubmit)
      .invoke(() -> waitForGraph.addAll(innerWaitForGraph))
      .flatMap(v -> putAllEdge(waitForGraph))
      .flatMap(v -> gsm.updateSubmittingStatus(ctx, ctxToSubmit)
        .map(TaskDetail::toRequest)
        .call(sender::send)
        .collect().last())
      .replaceWithVoid();
  }

  public Uni<Void> asyncSubmit(OaasObject obj) {
    MutableList<Map.Entry<OaasObject, OaasObject>> waitForGraph = Lists.mutable.empty();
    Set<TaskContext> ctxToSubmit = Sets.mutable.empty();
    waitForGraph.add(Map.entry(obj, OaasObjects.NULL));
    return traverseGraph(waitForGraph, ctxToSubmit)
      .flatMap(v -> putAllEdge(waitForGraph))
      .flatMap(v -> sender.send(ctxToSubmit.stream().map(TaskDetail::toRequest).toList()))
      .replaceWithVoid();
  }

  public Uni<InvApplyingContext> syncExec(InvApplyingContext ctx) {
    var output = ctx.getOutput();
    if (output != null)
      output.markAsSubmitted(null, false);
    var uni = syncInvoker.invoke(taskFactory.genTask(ctx));
    return uni
//      .flatMap(tc -> completionValidator.validateCompletion(ctx, tc))
//      .invoke(tc -> {
//        if (tc.getMain()!=null)
//          tc.getMain().update(ctx.getMain(), tc.getId().getVId());
//        if (output!=null)
//          output.updateStatus(tc);
//        ctx.setCompletion(tc);
//      })
      .flatMap(tc -> completionHandler.handleComplete(ctx, tc))
      .call(tc -> {
        List<OaasObject> list = tc.getMain()!=null ?
          Lists.mutable.of(ctx.getMain()):
          Lists.mutable.empty();
        return gsm.persistAll(ctx, list);
      })
      .onFailure(DataAccessException.class)
      .transform(InvocationException::detectConcurrent)
      .replaceWith(ctx);
  }


  public Uni<InvApplyingContext> asyncExec(InvApplyingContext ctx) {
    if (logger.isDebugEnabled())
      logger.debug("asyncExec {} {}", new TaskIdentity(ctx), ctx);
    var output = ctx.getOutput();
    if (output != null)
      output.markAsSubmitted(null, false);
    var uni = syncInvoker.invoke(taskFactory.genTask(ctx));
    return uni
//      .flatMap(tc -> completionValidator.validateCompletion(ctx, tc))
      .flatMap(tc -> completionHandler.handleComplete(ctx, tc))
//      .invoke(tc -> {
//        if (tc.getMain()!=null)
//          tc.getMain().update(ctx.getMain(), tc.getId().getVId());
//        if (output!=null)
//          output.updateStatus(tc);
//        ctx.setCompletion(tc);
//      })
      .call(tc -> this.complete(ctx,tc))
      .onFailure(DataAccessException.class).transform(InvocationException::detectConcurrent)
      .replaceWith(ctx);
  }

  public Uni<Void> complete(TaskCompletion completion) {
    return contextLoader.getTaskContextAsync(completion.getId().oId())
      .flatMap(ctx ->
//        completionValidator.validateCompletion(ctx, completion)
        completionHandler.handleComplete(ctx, completion)
        .map(cmp -> Tuples.pair(ctx, cmp))
      )
      .flatMap(pair -> gsm.persistThenLoadNext(pair.getOne(), pair.getTwo())
        .onItem().transformToUniAndConcatenate(o -> contextLoader.getTaskContextAsync(o))
        .collect().asList()
        .flatMap(list -> sender.send(list.stream().map(TaskDetail::toRequest).toList()))
      );
  }

  public Uni<Void> complete(TaskDetail task, TaskCompletion completion) {
//    logger.debug("complete {} {}", completion.getId(), completion);
    return gsm.persistThenLoadNext(task, completion)
      .onItem().transformToUniAndConcatenate(o -> contextLoader.getTaskContextAsync(o))
      .collect().asList()
      .flatMap(list -> sender.send(list.stream().map(TaskDetail::toRequest).toList()));
  }

  private Uni<Void> traverseGraph(List<Map.Entry<OaasObject, OaasObject>> waitForGraph,
                                  Set<TaskContext> ctxToSubmit) {
    if (waitForGraph.isEmpty())
      return Uni.createFrom().voidItem();
    return Multi.createBy().repeating()
      .uni(ResolveLoop::new, rl -> markOrExecRecursive(rl, waitForGraph, ctxToSubmit))
      .until(rl -> rl.i >= waitForGraph.size())
      .collect().last()
      .replaceWithVoid();
  }

  private Uni<ResolveLoop> markOrExecRecursive(ResolveLoop rl,
                                               List<Map.Entry<OaasObject, OaasObject>> waitForGraph,
                                               Set<TaskContext> ctxToSubmit) {
    if (rl.i >= waitForGraph.size()) {
      return Uni.createFrom().item(rl);
    }
    var entry = waitForGraph.get(rl.i++);
    OaasObject obj = entry.getKey();
    var ts = obj.getStatus().getTaskStatus();
    if (obj.isReadyToUsed() || ts.isFailed()) {
      return Uni.createFrom().item(rl);
    }
    if (!obj.getStatus().isInitWaitFor()) {
      return contextLoader.getTaskContextAsync(obj)
        .map(tc -> {
          List<Map.Entry<OaasObject, OaasObject>> subWfg = Lists.mutable.empty();
          var failDeps = Lists.mutable.<OaasObject>empty();
          if (tc.analyzeDeps(subWfg, failDeps)) {
            ctxToSubmit.add(tc);
          } else {
            waitForGraph.addAll(subWfg);
          }
          return subWfg;
        })
        .replaceWith(rl);
    }

    if (obj.getStatus().getWaitFor().isEmpty()) {
      return contextLoader.getTaskContextAsync(obj)
        .invoke(ctxToSubmit::add)
        .replaceWith(rl);
    }

    return Uni.createFrom().nullItem();
  }

  private Uni<Void> putAllEdge(MutableList<Map.Entry<OaasObject, OaasObject>> waitForGraph) {
    var edges = waitForGraph
      .select(e -> !OaasObjects.isNullObj(e.getValue()))
      .collect(entry ->
        Map.entry(entry.getKey().getId(), entry.getValue().getId()));
    return gsm.persistEdge(edges);
  }

  static class ResolveLoop {
    int i = 0;
  }
}
