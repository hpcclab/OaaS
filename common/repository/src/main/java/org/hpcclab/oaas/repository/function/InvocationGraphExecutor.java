package org.hpcclab.oaas.repository.function;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.hpcclab.oaas.model.TaskContext;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.function.OaasFunctionType;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.AggregateRepository;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;

//@ApplicationScoped
public class InvocationGraphExecutor {
  TaskSubmitter submitter;
  GraphStateManager gsm;
  ContextLoader contextLoader;

  @Inject
  public InvocationGraphExecutor(TaskSubmitter submitter,
                                 GraphStateManager gsm,
                                 ContextLoader contextLoader) {
    this.submitter = submitter;
    this.gsm = gsm;
  }


  public Uni<Void> markOrExec(FunctionExecContext ctx) {
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
    return markOrExec(waitForGraph, ctxToSubmit)
      .flatMap(v -> putAllEdge(waitForGraph))
      .flatMap(v -> submitter.submit(ctxToSubmit))
      .replaceWithVoid();
  }

  private Uni<Void> markOrExec(List<Map.Entry<OaasObject, OaasObject>> waitForGraph,
                               Set<TaskContext> ctxToSubmit) {
    return Multi.createBy().repeating()
      .uni(() -> 0, i -> markOrExecRecursive(i, waitForGraph, ctxToSubmit))
      .until(i -> i >= waitForGraph.size())
      .collect().last()
      .replaceWithVoid();
  }

  private Uni<Integer> markOrExecRecursive(int i,
                                           List<Map.Entry<OaasObject, OaasObject>> waitForGraph,
                                           Set<TaskContext> ctxToSubmit) {
    if (i >= waitForGraph.size()) {
      return Uni.createFrom().item(++i);
    }
    var entry = waitForGraph.get(i);
    OaasObject obj = entry.getKey();
    var ts = obj.getStatus().getTaskStatus();
    if (ts.isSubmitted() || ts.isFailed()) {
      return Uni.createFrom().item(++i);
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
        .replaceWith(++i);
    }

    if (obj.getStatus().getWaitFor().isEmpty()) {
      return contextLoader.getTaskContextAsync(obj)
        .invoke(ctxToSubmit::add)
        .replaceWith(++i);
    }

    return Uni.createFrom().nullItem();
  }

  public Uni<Void> putAllEdge(MutableList<Map.Entry<OaasObject, OaasObject>> waitForGraph) {
    var edges = waitForGraph.collect(entry -> Map.entry(entry.getKey().getId(), entry
      .getValue().getId()));
    return gsm.persistEdge(edges);
  }
}
