package org.hpcclab.oaas.arango.repo;

import com.arangodb.ArangoCollection;
import com.arangodb.async.ArangoCollectionAsync;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.arango.ObjectDependencyEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;
import java.util.Map;

import static org.hpcclab.oaas.arango.ConversionUtils.createUni;

@ApplicationScoped
public class ArgEdgeRepository extends AbstractArgRepository<ObjectDependencyEdge> {
  private static final Logger logger = LoggerFactory.getLogger(ArgEdgeRepository.class);
  @Inject
  @Named("OdeCollectionAsync")
  ArangoCollectionAsync edgeCollectionAsync;
  @Inject
  @Named("OdeCollection")
  ArangoCollection edgeCollection;
  @Named("ObjectCollectionAsync")
  ArangoCollectionAsync objCollection;


  @Override
  public ArangoCollection getCollection() {
    return edgeCollection;
  }

  @Override
  public ArangoCollectionAsync getAsyncCollection() {
    return edgeCollectionAsync;
  }

  @Override
  public Class<ObjectDependencyEdge> getValueCls() {
    return ObjectDependencyEdge.class;
  }

  @Override
  public String extractKey(ObjectDependencyEdge objectDependencyEdge) {
    return objectDependencyEdge.getId();
  }

  public ObjectDependencyEdge createEdge(String from, String to) {
    return ObjectDependencyEdge.of(objCollection.name() + "/" + from, objCollection.name() + "/" + to);
  }

  public Uni<List<String>> getAllEdge(String srcId) {
    var docId = objCollection.name() + "/" + srcId;
    logger.debug("getAllEdge[{}] {}", edgeCollection.name(), docId);
    // language=AQL
    var query = """
      FOR doc in @@edges
        FILTER doc._from == @src
        return doc
      """;
    var uni = createUni(() -> edgeCollectionAsync
      .db()
      .query(query,
        Map.of(
          "@edges", edgeCollectionAsync.name(),
          "src", docId
        ),
        ObjectDependencyEdge.class)
      .thenApply(cursor -> cursor.streamRemaining()
        .map(ode -> ode.getTo().substring(objCollection.name().length() + 1))
        .toList()
      )
    );

    if (logger.isDebugEnabled()) {
      uni = uni
        .invoke(list -> logger.debug("getAllEdge[{}] {} => {}",
          edgeCollection.name(), docId, list));
    }
    return uni;
  }
}
