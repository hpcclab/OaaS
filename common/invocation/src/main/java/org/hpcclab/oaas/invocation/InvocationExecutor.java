package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.invocation.task.TaskFactory;
import org.hpcclab.oaas.model.exception.DataAccessException;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.model.function.FunctionType;
import org.hpcclab.oaas.model.invocation.InternalInvocationNode;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.invocation.InvocationNode;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskIdentity;
import org.hpcclab.oaas.repository.GraphStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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

  public Uni<InvocationContext> syncExec(InvocationContext ctx) {
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

    ctx.initNode().markAsSubmitted(null, false);
    if (ctx.getRequest()!=null) {
      ctx.getNode().setQueTs(ctx.getRequest().queTs());
    }
    var uni = offLoader.offload(taskFactory.genTask(ctx));
    var uni2 = uni
      .flatMap(tc -> completionHandler.handleComplete(ctx, tc))
      .flatMap(tc -> this.finalizeCompletion(ctx, tc));
    if (logger.isDebugEnabled()) {
      uni2 = uni2.onFailure().invoke(e -> logger.debug("catch exception in invocation", e));
    }
    return uni2.onFailure(DataAccessException.class)
      .transform(InvocationException::detectConcurrent)
      .replaceWith(ctx);
  }

  public Uni<Void> finalizeCompletion(InvocationContext task, TaskCompletion completion) {
    return gsm.persistThenLoadNext(task, completion)
      .collect().asList()
      .flatMap(list -> sender.send(list));
  }
}
