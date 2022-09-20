package org.hpcclab.oaas.arango;

import com.arangodb.async.ArangoCollectionAsync;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.EntityRepository;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.hpcclab.oaas.repository.function.AbstractGraphStateManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ArgGraphStateManager extends AbstractGraphStateManager {
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
    return Uni.createFrom()
      .completionStage(future)
      .map(cursor -> cursor
        .stream()
        .map(ObjectDependencyEdge::getId)
        .toList());
  }

  @Override
  public Uni<Void> persistEdge(String srcId, String desId) {
    var ode = new ObjectDependencyEdge(srcId, desId);
    return Uni.createFrom().completionStage(
        edgeCollection.insertDocument(ode)
      )
      .replaceWithVoid();
  }

  @Override
  public Uni<Void> persistEdge(List<Map.Entry<String, String>> edgeMap) {
    var odes = edgeMap.stream()
      .map(e -> new ObjectDependencyEdge(e.getKey(), e.getValue()))
      .toList();
    return Uni.createFrom().completionStage(
        edgeCollection.insertDocuments(odes)
      )
      .replaceWithVoid();
  }
}
