package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.invocation.InvocationNode;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GraphStateManager {
  private static final Logger logger = LoggerFactory.getLogger(GraphStateManager.class);

  EntityRepository<String, InvocationNode> invNodeRepo;
  EntityRepository<String, OaasObject> objRepo;

  public GraphStateManager(EntityRepository<String, InvocationNode> invNodeRepo,
                           EntityRepository<String, OaasObject> objRepo) {
    this.invNodeRepo = invNodeRepo;
    this.objRepo = objRepo;
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
      .collect(Collectors.partitioningBy(o -> o.getRevision() <= 0));
    var newObjs = partitionedObjs.get(true);

    var oldObjs = partitionedObjs.get(false);

    if (oldObjs.isEmpty()) {
      return objRepo.persistAsync(newObjs);
    } else if (newObjs.isEmpty()) {
      return objRepo
        .atomic().persistWithPreconditionAsync(oldObjs);
    } else {
      return objRepo
        .atomic()
        .persistWithPreconditionAsync(oldObjs)
        .flatMap(__ -> objRepo.persistAsync(newObjs));
    }
  }

  public Multi<InvocationRequest> persistThenLoadNext(InvocationContext context, TaskCompletion completion) {

    var main = context.getMain();
    List<OaasObject> objs = new ArrayList<>();

    if (main!=null && !context.isImmutable()) {
      objs.add(main);
    }
    var out = context.getOutput();
    if (out!=null) {
      objs.add(out);
    }

    Uni<Void> uni = persistWithPrecondition(objs)
      .call(__ -> invNodeRepo.persistAsync(context.initNode()));
    return uni.onItem()
      .transformToMulti(__ -> {
        if (completion.isSuccess()) {
          return loadNextSubmittableNodes(context.initNode());
        } else {
          return handleFailed(context.initNode());
        }
      })
      .map(node -> node.toReq().build());

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
    if (srcNode.getNextInv().isEmpty()) {
      return Multi.createFrom().empty();
    }
    return invNodeRepo.listAsync(srcNode.getNextInv())
      .onItem()
      .transformToMulti(map -> Multi.createFrom().iterable(map.values()));
  }

  private Multi<InvocationNode> loadNextSubmittableNodes(InvocationNode srcNode) {
    logger.debug("loadNextSubmittableNodes {}", srcNode);
    if (srcNode.getNextInv().isEmpty()) {
      return Multi.createFrom().empty();
    }
    return Multi.createFrom().iterable(srcNode.getNextInv())
      .onItem()
      .transformToUniAndConcatenate(id -> invNodeRepo.computeAsync(id, (k, node) -> node.trigger(srcNode.getOriginator(), srcNode.getKey())))
      .filter(node -> Objects.equals(srcNode.getOriginator(), node.getOriginator()));
  }


  public Uni<Void> persistNodes(List<InvocationNode> nodes) {
    return invNodeRepo.persistAsync(nodes);
  }
}
