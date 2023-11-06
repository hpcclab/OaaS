package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.invocation.InvocationNode;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GraphStateManager {
  private static final Logger logger = LoggerFactory.getLogger(GraphStateManager.class);

  InvRepoManager invRepoManager;
  ObjectRepoManager objRepoManager;

  public GraphStateManager(InvRepoManager invRepoManager,
                           ObjectRepoManager objRepoManager) {
    this.invRepoManager = invRepoManager;
    this.objRepoManager = objRepoManager;
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
      return objRepoManager.persistAsync(newObjs);
    } else if (newObjs.isEmpty()) {
      return objRepoManager.persistWithRevAsync(oldObjs);
    } else {
      return objRepoManager
        .persistWithRevAsync(oldObjs)
        .flatMap(__ -> objRepoManager.persistAsync(newObjs));
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

    logger.debug("persist {}", objs);
    Uni<Void> uni = persistWithPrecondition(objs)
      .call(__ -> invRepoManager.persistAsync(context.initNode()));
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
      .transformToUniAndConcatenate(node -> invRepoManager
        .getOrCreate(node.getCls())
        .async()
        .computeAsync(node.getKey(), (k, v) -> v.markAsFailed()))
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
    return Multi.createFrom().iterable(srcNode.getNextInv())
      .onItem().transformToUniAndMerge(ref -> invRepoManager
        .getOrCreate(ref.getCls()).async().getAsync(ref.getKey())
      );
  }

  private Multi<InvocationNode> loadNextSubmittableNodes(InvocationNode srcNode) {
    logger.debug("loadNextSubmittableNodes {}", srcNode);
    if (srcNode.getNextInv().isEmpty()) {
      return Multi.createFrom().empty();
    }
    return Multi.createFrom().iterable(srcNode.getNextInv())
      .onItem()
      .transformToUniAndMerge(ref -> invRepoManager
        .getOrCreate(ref.getCls())
        .async()
        .computeAsync(ref.getKey(), (k, node) -> node.trigger(srcNode.getKey(), srcNode.getKey()))
      )
      .filter(node -> Objects.equals(srcNode.getKey(), node.getOriginator()));
  }


  public Uni<Void> persistNodes(List<InvocationNode> nodes) {
    return invRepoManager
      .persistAsync(nodes);
  }
}
