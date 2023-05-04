package org.hpcclab.oaas.invocation.dataflow;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.invocation.CompletedStateUpdater;
import org.hpcclab.oaas.invocation.OffLoader;
import org.hpcclab.oaas.invocation.task.TaskFactory;
import org.hpcclab.oaas.model.invocation.DataflowGraph;
import org.hpcclab.oaas.model.invocation.InvApplyingContext;
import org.hpcclab.oaas.model.invocation.InvocationNode;
import org.hpcclab.oaas.repository.GraphStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OneShotDataflowInvoker implements DataflowInvoker {
  private static final Logger logger = LoggerFactory.getLogger( OneShotDataflowInvoker.class );
  OffLoader offLoader;
  TaskFactory taskFactory;
  CompletedStateUpdater completedStateUpdater;
  GraphStateManager graphStateManager;

  @Inject
  public OneShotDataflowInvoker(OffLoader offLoader,
                                TaskFactory taskFactory,
                                CompletedStateUpdater completedStateUpdater,
                                GraphStateManager graphStateManager) {
    this.offLoader = offLoader;
    this.taskFactory = taskFactory;
    this.completedStateUpdater = completedStateUpdater;
    this.graphStateManager = graphStateManager;
  }

  @Override
  public Uni<InvApplyingContext> invoke(InvApplyingContext ctx) {
    var graph = ctx.getDataflowGraph();
    if (graph==null)
      throw new IllegalStateException();
    return Uni.createFrom().emitter(emitter -> {
        invokeNextReady(graph, emitter);
      })
      .replaceWith(ctx);
  }

  void invokeNextReady(DataflowGraph graph,
                       UniEmitter<?> emitter) {
    var readyNodes = graph.findNextExecutable(true);
    if (readyNodes.isEmpty()) {
      if (graph.isAllCompleted()) {
        handleCompletedDataflow(graph, emitter);
      }
      return;
    }
    if (graph.isFail())
      return;
    for (InvocationNode node : readyNodes) {
      var ctx = node.getCtx();
      var task = taskFactory.genTask(ctx);
      if (ctx.getRequest() != null && task.getOutput() != null)
        task.getOutput().getStatus().setQueTs(ctx.getRequest().queTs());
      offLoader.offload(task)
        .flatMap(cmp -> completedStateUpdater.handleComplete(ctx, cmp))
        .invoke(() -> node.setCompleted(true))
        .subscribe()
        .with(
          cmp -> invokeNextReady(graph, emitter),
          err -> {
            graph.setFailed(true);
            emitter.fail(err);
          }
        );
    }
  }

  void handleCompletedDataflow(DataflowGraph graph, UniEmitter<?> emitter) {
    var ctx = graph.getTop();
    var main = ctx.getMain();
    graphStateManager.persistAll(ctx, Lists.mutable.of(main))
      .subscribe()
      .with(item -> {
        emitter.complete(null);
      }, emitter::fail);
  }
}
