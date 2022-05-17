package org.hpcclab.oaas.repository.function;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.hpcclab.oaas.model.TaskContext;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.function.OaasFunctionType;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;

//@ApplicationScoped
public class InvocationGraphExecutor {
  private static final Logger LOGGER = LoggerFactory.getLogger(InvocationGraphExecutor.class);
  TaskSubmitter submitter;
  GraphStateManager gsm;
  ContextLoader contextLoader;

  @Inject
  public InvocationGraphExecutor(TaskSubmitter submitter,
                                 GraphStateManager gsm,
                                 ContextLoader contextLoader) {
    this.submitter = submitter;
    this.gsm = gsm;
    this.contextLoader = contextLoader;
  }


  public Uni<Void> exec(FunctionExecContext ctx) {
    Set<TaskContext> ctxToSubmit = Sets.mutable.empty();
    MutableList<Map.Entry<OaasObject, OaasObject>> waitForGraph = Lists.mutable.empty();
    var failDeps = Lists.mutable.<OaasObject>empty();
    if (ctx.analyzeDeps(waitForGraph, failDeps)) {
      switch (ctx.getFunction().getType()) {
        case MACRO -> {
          for (var subCtx : ctx.getSubContexts()) {
            if (subCtx.getFunction().getType()==OaasFunctionType.TASK
              && subCtx.analyzeDeps(waitForGraph, failDeps))
              ctxToSubmit.add(subCtx);
          }
        }
        case TASK -> ctxToSubmit.add(ctx);
        case LOGICAL -> {
        } // DO NOTHING
      }
    }
    return traverseGraph(waitForGraph, ctxToSubmit)
      .flatMap(v -> putAllEdge(waitForGraph))
      .flatMap(v -> gsm.updateSubmitStatus(ctx, ctxToSubmit)
        .collect().asList())
      .flatMap(submittableContexts -> submitter.submit(submittableContexts))
      .replaceWithVoid();
  }

  public Uni<Void> exec(OaasObject obj) {
    MutableList<Map.Entry<OaasObject, OaasObject>> waitForGraph = Lists.mutable.empty();
    Set<TaskContext> ctxToSubmit = Sets.mutable.empty();
    waitForGraph.add(Map.entry(obj, null));
    return traverseGraph(waitForGraph, ctxToSubmit)
      .flatMap(v -> putAllEdge(waitForGraph))
      .flatMap(v -> submitter.submit(ctxToSubmit))
      .replaceWithVoid();
  }

  public Uni<Void> complete(TaskCompletion completion) {
    return contextLoader.getObject(completion.getId())
      .onItem().ifNull().failWith(() -> NoStackException.notFoundObject400(completion.getId()))
      .invoke(o -> {
        o.getStatus().set(completion);
        o.setEmbeddedRecord(completion.getEmbeddedRecord());
      })
      .onItem().transformToMulti(o -> gsm.handleComplete(o))
      .onItem().transformToUniAndConcatenate(o -> contextLoader.getTaskContextAsync(o))
      .collect().asList()
      .flatMap(list -> submitter.submit(list));
  }

  private Uni<Void> traverseGraph(List<Map.Entry<OaasObject, OaasObject>> waitForGraph,
                                  Set<TaskContext> ctxToSubmit) {
//    System.out.println("waitForGraph:"+ waitForGraph);
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
    if (ts.isSubmitted() || ts.isFailed()) {
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
    var edges = waitForGraph.collect(entry -> Map.entry(entry.getKey().getId(), entry
      .getValue().getId()));
    return gsm.persistEdge(edges);
  }

  static class ResolveLoop {
    int i = 0;

  }
}
