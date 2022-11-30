package org.hpcclab.oaas.arango;

import com.arangodb.async.ArangoCollectionAsync;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.repository.AbstractGraphStateManager;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

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
  @Named("OdeCollectionAsync")
  ArangoCollectionAsync edgeCollection;
  @Named("ObjectCollectionAsync")
  ArangoCollectionAsync objCollection;


  @Inject
  public ArgGraphStateManager(ObjectRepository objRepo) {
    super(objRepo);
  }

  @Override
  public Uni<Collection<String>> getAllEdge(String srcId) {
    LOGGER.debug("getAllEdge[{}] {}",
      edgeCollection.name(), srcId);
    var docId = objCollection.name() + "/" + srcId;
    // langauge=AQL
    var query = """
      FOR doc in @@edges
        FILTER doc._from == @src
        return doc
      """;
    var future = objCollection
      .db()
      .query(query,
        Map.of("@edges", edgeCollection.name(),
          "src", docId),
        ObjectDependencyEdge.class);
    return createUni(future)
      .map(cursor -> cursor
        .stream()
        .map(ObjectDependencyEdge::getId)
        .toList());
  }

  @Override
  public Uni<Void> persistEdge(String srcId, String desId) {
    if (LOGGER.isDebugEnabled())
      LOGGER.debug("persistEdge[{}] {}", edgeCollection.name(), srcId);
    var ode = new ObjectDependencyEdge(srcId, desId);
    return createUni(edgeCollection.insertDocument(ode))
      .replaceWithVoid();
  }

  @Override
  public Uni<Void> persistEdge(List<Map.Entry<String, String>> edgeMap) {
    if (LOGGER.isDebugEnabled())
      LOGGER.debug("persistEdge(col)[{}] {}", edgeCollection.name(), edgeMap.size());
    if (edgeMap.isEmpty())
      return Uni.createFrom().voidItem();
    var odes = edgeMap.stream()
      .map(e -> new ObjectDependencyEdge(e.getKey(), e.getValue()))
      .toList();
    return createUni(edgeCollection.insertDocuments(odes))
      .replaceWithVoid();
  }

  protected <T> Uni<T> createUni(CompletionStage<T> stage) {
    var uni = Uni.createFrom().completionStage(stage);
    var ctx = Vertx.currentContext();
    if (ctx!=null)
      return uni.emitOn(ctx::runOnContext);
    return uni;
  }
}
