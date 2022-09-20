package org.hpcclab.oaas.arango;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.async.ArangoCollectionAsync;
import com.arangodb.entity.CollectionType;
import com.arangodb.entity.EdgeDefinition;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.model.GraphCreateOptions;
import com.arangodb.model.PersistentIndexOptions;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

@ApplicationScoped
public class ArgRepositoryInitializer {

  @Inject
  ArangoDatabase database;
  @Inject
  @Named("ObjectCollection")
  ArangoCollection objCol;
  @Inject
  @Named("FunctionCollection")
  ArangoCollection funcCol;
  @Inject
  @Named("ClassCollection")
  ArangoCollection clsCol;
  @Inject
  @Named("OdeCollectionAsync")
  ArangoCollectionAsync odeColAsync;

  public void setup() {
    if (!database.exists())
      database.create();
    if (!objCol.exists()) {
      objCol.create(new CollectionCreateOptions()
        .numberOfShards(3)
        .replicationFactor(2)
        .writeConcern(1));
      objCol.ensurePersistentIndex(List.of("cls"),
        new PersistentIndexOptions());
    }
    if (!funcCol.exists()) {
      funcCol.create(new CollectionCreateOptions()
        .numberOfShards(3)
        .replicationFactor(2)
        .writeConcern(1));
    }
    if (!clsCol.exists()) {
      clsCol.create(new CollectionCreateOptions()
        .numberOfShards(3)
        .replicationFactor(2)
        .writeConcern(1));
    }
    if (!database.collection(odeColAsync.name()).exists()) {
      database.createCollection(odeColAsync.name(), new CollectionCreateOptions()
        .numberOfShards(3)
        .replicationFactor(2)
        .type(CollectionType.EDGES)
        .writeConcern(1));
    }
    var graph = database.graph("OaasGraph");
    if (!graph.exists()) {
      graph.create(List.of(
          new EdgeDefinition().
            collection(odeColAsync.name())
            .from(objCol.name())
            .to(objCol.name())
        ),
        new GraphCreateOptions()
          .numberOfShards(3)
          .replicationFactor(2)
      );
    }
  }
}
