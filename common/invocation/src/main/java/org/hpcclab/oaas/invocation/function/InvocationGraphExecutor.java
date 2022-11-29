package org.hpcclab.oaas.invocation.function;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.hpcclab.oaas.invocation.ContextLoader;
import org.hpcclab.oaas.invocation.SyncInvoker;
import org.hpcclab.oaas.model.TaskContext;
import org.hpcclab.oaas.model.function.DeploymentCondition;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.function.FunctionType;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.object.OaasObjects;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.repository.GraphStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InvocationGraphExecutor {
  private static final Logger LOGGER = LoggerFactory.getLogger(InvocationGraphExecutor.class);
  TaskSubmitter submitter;
  GraphStateManager gsm;
  ContextLoader contextLoader;
  SyncInvoker syncInvoker;


  @Inject
  public InvocationGraphExecutor(TaskSubmitter submitter,
                                 GraphStateManager gsm,
                                 ContextLoader contextLoader,
                                 SyncInvoker syncInvoker) {
    this.submitter = submitter;
    this.gsm = gsm;
    this.contextLoader = contextLoader;
    this.syncInvoker = syncInvoker;
  }


  public boolean canSyncInvoke(FunctionExecContext ctx) {
    var func = ctx.getFunction();
    if (func.getType() != FunctionType.TASK){
      return false;
    }
    if (func.getDeploymentStatus().getCondition() != DeploymentCondition.RUNNING) {
      return false;
    }
    MutableList<Map.Entry<OaasObject, OaasObject>> waitForGraph =
      Lists.mutable.empty();
    MutableList<OaasObject> failDeps = Lists.mutable.empty();
    return ctx.analyzeDeps(waitForGraph, failDeps);
  }

  public Uni<Void> exec(FunctionExecContext ctx) {
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
        case TASK -> ctxToSubmit.add(ctx);
        case LOGICAL -> {
        } // DO NOTHING
      }
    }
    return traverseGraph(waitForGraph, ctxToSubmit)
      .invoke(() -> waitForGraph.addAll(innerWaitForGraph))
      .flatMap(v -> putAllEdge(waitForGraph))
      .flatMap(v -> gsm.updateSubmittingStatus(ctx, ctxToSubmit)
        .call(submitter::submit)
        .collect().last())
      .replaceWithVoid();
  }

  public Uni<Void> exec(OaasObject obj) {
    MutableList<Map.Entry<OaasObject, OaasObject>> waitForGraph = Lists.mutable.empty();
    Set<TaskContext> ctxToSubmit = Sets.mutable.empty();
    waitForGraph.add(Map.entry(obj, OaasObjects.NULL));
    return traverseGraph(waitForGraph, ctxToSubmit)
      .flatMap(v -> putAllEdge(waitForGraph))
      .flatMap(v -> submitter.submit(ctxToSubmit))
      .replaceWithVoid();
  }

  public Uni<FunctionExecContext> syncExec(FunctionExecContext ctx) {
    var output = ctx.getOutput();
    output.markAsSubmitted(null, false);
    var uni = syncInvoker.invoke(ctx);
    return uni.invoke(output::updateStatus)
      .call(() -> gsm.persistAllWithoutNoti(ctx))
      .replaceWith(ctx);
  }

  public Uni<Void> complete(TaskCompletion completion) {
    return gsm.handleComplete(completion)
      .onItem().transformToUniAndConcatenate(o -> contextLoader.getTaskContextAsync(o))
      .collect().asList()
      .flatMap(list -> submitter.submit(list));
  }

  public Uni<Void> complete(OaasTask task, TaskCompletion completion) {
    return gsm.handleComplete(task, completion)
      .onItem().transformToUniAndConcatenate(o -> contextLoader.getTaskContextAsync(o))
      .collect().asList()
      .flatMap(list -> submitter.submit(list));
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
