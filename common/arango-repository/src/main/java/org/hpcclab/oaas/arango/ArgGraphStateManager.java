package org.hpcclab.oaas.arango;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.arango.repo.ArgEdgeRepository;
import org.hpcclab.oaas.repository.AbstractGraphStateManager;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@RegisterForReflection(
  targets = {
    ObjectDependencyEdge.class
  },
  registerFullHierarchy = true
)
public class ArgGraphStateManager extends AbstractGraphStateManager {
  private static final Logger LOGGER = LoggerFactory.getLogger( ArgGraphStateManager.class );
  @Inject
  ArgEdgeRepository odeRepo;


  @Inject
  public ArgGraphStateManager(ObjectRepository objRepo) {
    super(objRepo);
  }

  @Override
  public Uni<? extends Collection<String>> getAllEdge(String srcId) {
    return odeRepo.getAllEdge(srcId);
  }

  @Override
  public Uni<Void> persistEdge(String srcId, String desId) {
    if (LOGGER.isDebugEnabled())
      LOGGER.debug("persistEdge[{}] {}", odeRepo.getCollection().name(), srcId);
    var ode = odeRepo.createEdge(srcId, desId);
    return odeRepo.persistAsync(ode)
      .replaceWithVoid();
  }

  @Override
  public Uni<Void> persistEdge(List<Map.Entry<String, String>> edgeMap) {
    if (edgeMap.isEmpty())
      return Uni.createFrom().voidItem();
    var odes = edgeMap.stream()
      .map(e -> odeRepo.createEdge(e.getKey(),e.getValue()))
      .toList();
    if (LOGGER.isDebugEnabled())
      LOGGER.debug("persistEdge(col)[{}] {}", odeRepo.getCollection().name(), odes);
    return odeRepo.persistAsync(odes);
  }
}
