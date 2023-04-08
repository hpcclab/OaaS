package org.hpcclab.oaas.invoker.ispn;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invoker.ispn.edge.ObjInvNode;
import org.hpcclab.oaas.invoker.ispn.repo.EmbeddedIspnInvNodeRepository;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.AbstractGraphStateManager;
import org.hpcclab.oaas.repository.EntityRepository;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class IspnGraphStateManager extends AbstractGraphStateManager {
  private static final Logger logger = LoggerFactory.getLogger( IspnGraphStateManager.class );

  EmbeddedIspnInvNodeRepository nodeRepo;

  @Inject
  public IspnGraphStateManager(ObjectRepository objRepo,
                               EmbeddedIspnInvNodeRepository nodeRepo) {
    super(objRepo);
    this.nodeRepo = nodeRepo;
  }

  @Override
  public Uni<? extends Collection<String>> getAllEdge(String srcId) {
    logger.debug("getAllEdge {}", srcId);
    return nodeRepo.getAsync(srcId)
      .map(node -> {
//        logger.debug("InvNode {} {}", srcId, node);
        if (node==null) return List.of();
        return node.getNextInv();
      });
  }

  @Override
  public Uni<Void> persistEdge(String srcId, String desId) {
    logger.debug("persistEdge {}, {}", srcId, desId);
    return nodeRepo.computeAsync(srcId, (k, node) -> {
//        logger.debug("InvNode1 {} {}", srcId, node);
        if (node==null)
          node = new ObjInvNode(k);
        node.getNextInv().add(desId);
//        logger.debug("InvNode2 {} {}", srcId, node);
        return node;
      })
      .replaceWithVoid();
  }

  @Override
  public Uni<Void> persistEdge(List<Map.Entry<String, String>> edgeMap) {
    logger.debug("persistEdges {}", edgeMap);
    return Multi.createFrom().iterable(edgeMap)
      .onItem().transformToUniAndMerge(entry -> persistEdge(entry.getKey(), entry.getValue())
        )
      .collect().last()
      .replaceWithVoid();
  }
}
