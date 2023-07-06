package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.invocation.applier.UnifiedFunctionRouter;
import org.hpcclab.oaas.invocation.task.TaskFactory;
import org.hpcclab.oaas.model.exception.DataAccessException;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.model.function.FunctionType;
import org.hpcclab.oaas.model.invocation.InternalInvocationNode;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.invocation.InvocationNode;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskContext;
import org.hpcclab.oaas.model.task.TaskDetail;
import org.hpcclab.oaas.model.task.TaskIdentity;
import org.hpcclab.oaas.repository.GraphStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class InvocationExecutor {
  private static final Logger logger = LoggerFactory.getLogger(InvocationExecutor.class);
  InvocationQueueSender sender;
  GraphStateManager gsm;
  ContextLoader contextLoader;
  OffLoader offLoader;
  CompletedStateUpdater completionHandler;
  TaskFactory taskFactory;


  public InvocationExecutor(InvocationQueueSender sender,
                            GraphStateManager gsm,
                            ContextLoader contextLoader,
                            OffLoader offLoader,
                            TaskFactory taskFactory,
                            CompletedStateUpdater completionHandler) {
    this.sender = sender;
    this.gsm = gsm;
    this.contextLoader = contextLoader;
    this.offLoader = offLoader;
    this.taskFactory = taskFactory;
    this.completionHandler = completionHandler;
  }

//  public boolean canSyncInvoke(InvocationContext ctx) {
//    var func = ctx.getFunction();
//    if (func.getType()==FunctionType.MACRO) {
//      return false;
//    }
//    if (func.getDeploymentStatus().getCondition()!=DeploymentCondition.RUNNING) {
//      return false;
//    }
//    MutableList<Map.Entry<OaasObject, OaasObject>> waitForGraph =
//      Lists.mutable.empty();
//    MutableList<OaasObject> failDeps = Lists.mutable.empty();
//    return ctx.analyzeDeps(waitForGraph, failDeps);
//  }

//  public Uni<Void> asyncSubmit(InvocationContext ctx) {
//    Set<TaskContext> ctxToSubmit = Sets.mutable.empty();
//    MutableList<Map.Entry<OaasObject, OaasObject>> waitForGraph =
//      Lists.mutable.empty();
//    MutableList<Map.Entry<OaasObject, OaasObject>> innerWaitForGraph =
//      Lists.mutable.empty();
//    MutableList<OaasObject> failDeps = Lists.mutable.empty();
//    if (ctx.analyzeDeps(waitForGraph, failDeps)) {
//      switch (ctx.getFunction().getType()) {
//        case MACRO -> {
//          for (var subCtx : ctx.getSubContexts()) {
//            if (subCtx.getFunction().getType()==FunctionType.TASK
//              && subCtx.analyzeDeps(innerWaitForGraph, failDeps))
//              ctxToSubmit.add(subCtx);
//          }
//        }
//        case TASK, IM_TASK -> ctxToSubmit.add(ctx);
//        default -> {
//          // DO NOTHING
//        }
//      }
//    }
//
//    return traverseGraph(waitForGraph, ctxToSubmit)
//      .invoke(() -> waitForGraph.addAll(innerWaitForGraph))
//      .flatMap(v -> putAllEdge(waitForGraph))
//      .flatMap(v -> gsm.updateSubmittingStatus(ctx, ctxToSubmit)
//        .map(TaskDetail::toRequest)
//        .call(sender::send)
//        .collect().last())
//      .replaceWithVoid();
//  }

  public Uni<Void> asyncSubmit(InvocationContext ctx) {
    Set<InvocationContext> ctxToSubmit;
    List<InvocationNode> nodes;
    if (ctx.getFunction().getType()==FunctionType.MACRO) {
      nodes = ctx.getDataflowGraph().exportGraph();
      ctxToSubmit = ctx.getDataflowGraph().findNextExecutable(false)
        .stream()
        .map(InternalInvocationNode::getCtx)
        .collect(Collectors.toSet());
    } else {
      nodes = List.of(ctx.initNode());
      ctxToSubmit = Set.of(ctx);
    }

    return gsm.persistNodes(nodes)
      .onItem()
      .transformToMulti(__ -> gsm.updateSubmittingStatus(ctx, ctxToSubmit))
      .map(InvocationContext::toRequest)
      .collect().asList()
      .flatMap(sender::send);
  }

//  public Uni<Void> asyncSubmit(OaasObject obj) {
//    MutableList<Map.Entry<OaasObject, OaasObject>> waitForGraph = Lists.mutable.empty();
//    Set<TaskContext> ctxToSubmit = Sets.mutable.empty();
//    waitForGraph.add(Map.entry(obj, OaasObjects.NULL));
//    return traverseGraph(waitForGraph, ctxToSubmit)
//      .flatMap(v -> putAllEdge(waitForGraph))
//      .flatMap(v -> sender.send(ctxToSubmit.stream().map(TaskDetail::toRequest).toList()))
//      .replaceWithVoid();
//  }

  public Uni<InvocationContext> syncExec(InvocationContext ctx) {
//    var output = ctx.getOutput();
//    if (output!=null)
//      output.markAsSubmitted(null, false);
    ctx.initNode()
      .markAsSubmitted(null, false);
    var uni = offLoader.offload(taskFactory.genTask(ctx));
    return uni
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


  public Uni<InvocationContext> asyncExec(InvocationContext ctx) {
    if (logger.isDebugEnabled())
      logger.debug("asyncExec {} {}", new TaskIdentity(ctx), ctx);
//    var output = ctx.getOutput();
//    if (output!=null) {
//      output.markAsSubmitted(null, false);
//      if (ctx.getRequest()!=null)
//        output.getStatus().setQueTs(ctx.getRequest().queTs());
//    }

    ctx.initNode().markAsSubmitted(null, false);
    if (ctx.getRequest() != null) {
      ctx.getNode().setQueTs(ctx.getRequest().queTs());
    }
    var uni = offLoader.offload(taskFactory.genTask(ctx));
    return uni
      .flatMap(tc -> completionHandler.handleComplete(ctx, tc))
      .call(tc -> this.complete(ctx, tc))
      .onFailure(DataAccessException.class).transform(InvocationException::detectConcurrent)
      .replaceWith(ctx);
  }

  public Uni<Void> complete(InvocationContext task, TaskCompletion completion) {
    return gsm.persistThenLoadNext(task, completion)
      .collect().asList()
      .flatMap(list -> sender.send(list));
  }

//  private Uni<Void> traverseGraph(List<Map.Entry<OaasObject, OaasObject>> waitForGraph,
//                                  Set<TaskContext> ctxToSubmit) {
//    if (waitForGraph.isEmpty())
//      return Uni.createFrom().voidItem();
//    return Multi.createBy().repeating()
//      .uni(ResolveLoop::new, rl -> markOrExecRecursive(rl, waitForGraph, ctxToSubmit))
//      .until(rl -> rl.i >= waitForGraph.size())
//      .collect().last()
//      .replaceWithVoid();
//  }
//
//  private Uni<ResolveLoop> markOrExecRecursive(ResolveLoop rl,
//                                               List<Map.Entry<OaasObject, OaasObject>> waitForGraph,
//                                               Set<TaskContext> ctxToSubmit) {
//    if (rl.i >= waitForGraph.size()) {
//      return Uni.createFrom().item(rl);
//    }
//    var entry = waitForGraph.get(rl.i++);
//    OaasObject obj = entry.getKey();
//    var ts = obj.getStatus().getTaskStatus();
//    if (obj.isReadyToUsed() || ts.isFailed()) {
//      return Uni.createFrom().item(rl);
//    }
//    if (!obj.getStatus().isInitWaitFor()) {
//      return contextLoader.getTaskContextAsync(obj)
//        .map(tc -> {
//          List<Map.Entry<OaasObject, OaasObject>> subWfg = Lists.mutable.empty();
//          var failDeps = Lists.mutable.<OaasObject>empty();
//          if (tc.analyzeDeps(subWfg, failDeps)) {
//            ctxToSubmit.add(tc);
//          } else {
//            waitForGraph.addAll(subWfg);
//          }
//          return subWfg;
//        })
//        .replaceWith(rl);
//    }
//
//    if (obj.getStatus().getWaitFor().isEmpty()) {
//      return contextLoader.getTaskContextAsync(obj)
//        .invoke(ctxToSubmit::add)
//        .replaceWith(rl);
//    }
//
//    return Uni.createFrom().nullItem();
//  }

//  private Uni<Void> putAllEdge(MutableList<Map.Entry<OaasObject, OaasObject>> waitForGraph) {
//    var edges = waitForGraph
//      .select(e -> !OaasObjects.isNullObj(e.getValue()))
//      .collect(entry ->
//        Map.entry(entry.getKey().getId(), entry.getValue().getId()));
//    return gsm.persistNodes(edges);
//  }

  static class ResolveLoop {
    int i = 0;
  }
}
