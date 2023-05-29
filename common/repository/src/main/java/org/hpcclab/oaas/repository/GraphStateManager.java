package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.invocation.InvocationNode;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskContext;
import org.hpcclab.oaas.model.task.TaskDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class GraphStateManager {
  private static final Logger logger = LoggerFactory.getLogger( GraphStateManager.class );

  EntityRepository<String, InvocationNode> invNodeRepo;
  EntityRepository<String, OaasObject> objRepo;

  public GraphStateManager(EntityRepository<String, InvocationNode> invNodeRepo,
                           EntityRepository<String, OaasObject> objRepo) {
    this.invNodeRepo = invNodeRepo;
    this.objRepo = objRepo;
  }

  public Multi<InvocationContext> updateSubmittingStatus(InvocationContext entryCtx, Collection<InvocationContext> contexts){
    var originator = entryCtx.getOutput()!=null ?
      entryCtx.getOutput().getId()
      :entryCtx.getMain().getId();
    return Multi.createFrom().iterable(contexts)
      .onItem().transformToUniAndConcatenate(ctx -> {
        if (ctx.getOutput()==null) {
          return Uni.createFrom().item(ctx);
        }
        if (entryCtx.contains(ctx)) {
          ctx.initNode().markAsSubmitted(originator, true);
          return Uni.createFrom().item(ctx);
        } else {
          return invNodeRepo.computeAsync(ctx.getOutput().getId(), (id, node) ->
              node.markAsSubmitted(originator, true))
            .invoke(ctx::setNode)
            .replaceWith(ctx);
        }
      })
      .filter(ctx -> ctx.getOutput()==null || ctx.getNode().getOriginator().equals(originator))
      .onCompletion().call(() -> persistAllInvNodes(entryCtx))
      ;
  }

  public Uni<Void> persistAll(InvocationContext ctx) {
    return persistAll(ctx, Lists.mutable.empty());
  }

  public Uni<Void> persistAllInvNodes(InvocationContext ctx) {
    var nodes = Lists.mutable.<InvocationNode>empty();
    var n = ctx.getNode();
    if (n != null)
      nodes.add(ctx.getNode());
    nodes.addAll(ctx.getSubContexts().stream().map(TaskContext::getNode)
      .filter(Objects::nonNull).toList());
    return invNodeRepo.persistAsync(nodes);
  }

  public Uni<Void> persistAll(InvocationContext ctx, List<OaasObject> objs) {
    objs.addAll(ctx.getSubOutputs());
    var dataflow = ctx.getFunction().getMacro();
    if (ctx.getOutput()!=null && (dataflow==null || dataflow.getExport()==null)) {
      objs.add(ctx.getOutput());
    }
    return persistWithPrecondition(objs);
  }

  private Uni<Void> persistWithPrecondition(List<OaasObject> objs) {
    var partitionedObjs = objs.stream()
      .collect(Collectors.partitioningBy(o -> o.getRev()==null));
    var newObjs = partitionedObjs.get(true);

    var oldObjs = partitionedObjs.get(false);

    if (oldObjs.isEmpty()) {
      return objRepo.persistAsync(newObjs);
    } else if (newObjs.isEmpty()) {
      return objRepo.atomic().persistWithPreconditionAsync(oldObjs);
    } else {
      return objRepo
        .atomic().persistWithPreconditionAsync(oldObjs)
        .flatMap(__ -> objRepo.persistAsync(newObjs));
    }
  }

  public Multi<InvocationRequest> persistThenLoadNext(TaskDetail task, TaskCompletion completion) {

    var main = task.getMain();
    List<OaasObject> objs = new ArrayList<>();

    if (main!=null) {
      objs.add(main);
    }
    var out = task.getOutput();
    if (out!=null) {
      objs.add(out);
    }

    Uni<Void> uni = persistWithPrecondition(objs);
    if (out==null)
      return uni.onItem().transformToMulti(__ -> Multi.createFrom().empty());

    else {
      return uni.onItem()
        .transformToMulti(__ -> {
          if (completion.isSuccess()) {
            return loadNextSubmittableNodes(task.getNode());
          } else {
            return handleFailed(task.getNode());
          }
        })
        .map(InvocationNode::toReq);
    }
  }

  private Multi<InvocationNode> handleFailed(InvocationNode failedNode) {
    return getDependentsRecursive(failedNode)
      .onItem()
      .transformToUniAndConcatenate(node -> invNodeRepo.computeAsync(node.getKey(), (k, v) -> v.markAsFailed()))
      .filter(i -> false);

  }

  public Multi<InvocationNode> getDependentsRecursive(InvocationNode srcNode) {
    return loadNextNodes(srcNode)
      .flatMap(node -> getDependentsRecursive(node)
        .onCompletion().continueWith(node));
  }

  private Multi<InvocationNode> loadNextNodes(InvocationNode srcNode) {
    if (srcNode.getNextInv() == null || srcNode.getNextInv().isEmpty()) {
      return Multi.createFrom().empty();
    }
    return invNodeRepo.listAsync(srcNode.getNextInv())
      .onItem()
      .transformToMulti(map -> Multi.createFrom().iterable(map.values()));
  }
  private Multi<InvocationNode> loadNextSubmittableNodes(InvocationNode srcNode) {
    if (srcNode.getNextInv() == null || srcNode.getNextInv().isEmpty()) {
      return Multi.createFrom().empty();
    }
    return Multi.createFrom().iterable(srcNode.getNextInv())
      .onItem()
      .transformToUniAndConcatenate(id -> invNodeRepo.computeAsync(id, (k, node) -> node.trigger(srcNode.getOriginator(), srcNode.getKey())))
      .filter(node -> Objects.equals(srcNode.getOriginator(), node.getOriginator()));
  }


  public Uni<Void> persistNodes(List<InvocationNode> nodes){
    return invNodeRepo.persistAsync(nodes);
  }
}
