package org.hpcclab.oaas.arango.repo;

import com.arangodb.ArangoCollection;
import com.arangodb.async.ArangoCollectionAsync;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.arango.ObjectDependencyEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.Map;

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
    return objectDependencyEdge.id();
  }

  public ObjectDependencyEdge createEdge(String from, String to) {
    return ObjectDependencyEdge.of(objCollection.name() + "/" + from, objCollection.name() + "/" + to);
  }

  public Uni<Collection<String>> getAllEdge(String srcId) {
    logger.debug("getAllEdge[{}] {}",
      edgeCollection.name(), srcId);
    var docId = objCollection.name() + "/" + srcId;
    // language=AQL
    var query = """
      FOR doc in @@edges
        FILTER doc._from == CONCAT(@objCol,"/", @src)
        return doc
      """;
    return createUni(() -> objCollection
      .db()
      .query(query,
        Map.of(
          "@edges", edgeCollection.name(),
          "src", docId,
          "objCol", objCollection.name()
        ),
        ObjectDependencyEdge.class))
      .map(cursor -> cursor
        .stream()
        .map(ObjectDependencyEdge::id)
        .toList());
  }
}
