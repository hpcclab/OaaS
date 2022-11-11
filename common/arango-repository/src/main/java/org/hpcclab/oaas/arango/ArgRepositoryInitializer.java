package org.hpcclab.oaas.arango;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.ArangoView;
import com.arangodb.async.ArangoCollectionAsync;
import com.arangodb.entity.CollectionType;
import com.arangodb.entity.EdgeDefinition;
import com.arangodb.entity.arangosearch.CollectionLink;
import com.arangodb.entity.arangosearch.FieldLink;
import com.arangodb.entity.arangosearch.PrimarySort;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.model.GraphCreateOptions;
import com.arangodb.model.PersistentIndexOptions;
import com.arangodb.model.arangosearch.ArangoSearchCreateOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

@ApplicationScoped
public class ArgRepositoryInitializer {
  private static final Logger LOGGER = LoggerFactory.getLogger(ArgRepositoryInitializer.class);

  @Inject
  ArangoDatabase database;
  @Inject
  @Named("ObjectCollection")
  ArangoCollection objCol;
  @Inject
  @Named("ObjectView")
  ArangoView objView;
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
    if (!database.exists()) database.create();
    if (!objCol.exists()) {
      objCol.create(new CollectionCreateOptions().numberOfShards(3).replicationFactor(2).writeConcern(1));
      objCol.ensurePersistentIndex(List.of("cls"), new PersistentIndexOptions());
    }

    if (!funcCol.exists()) {
      funcCol.create(new CollectionCreateOptions().numberOfShards(3).replicationFactor(2).writeConcern(1));
    }
    if (!clsCol.exists()) {
      clsCol.create(new CollectionCreateOptions().numberOfShards(3).replicationFactor(2).writeConcern(1));
    }
    if (!database.collection(odeColAsync.name()).exists()) {
      database.createCollection(odeColAsync.name(), new CollectionCreateOptions().numberOfShards(3).replicationFactor(2).type(CollectionType.EDGES).writeConcern(1));
    }
    createObjectView();
    var graph = database.graph("OaasGraph");
    if (!graph.exists()) {
      graph.create(List.of(new EdgeDefinition().collection(odeColAsync.name()).from(objCol.name()).to(objCol.name())), new GraphCreateOptions().numberOfShards(3).replicationFactor(2));
    }
  }

  void createObjectView() {
    var as = database.arangoSearch(objView.name());
    var exist = objView.exists();
    LOGGER.info("ObjectView exists {}", exist);
    if (!exist) {
      var view = as.create(
        new ArangoSearchCreateOptions()
          .link(CollectionLink.on(objCol.name())
            .analyzers("identity")
            .fields(
              FieldLink.on("_key"),
              FieldLink.on("cls"),
              FieldLink.on("status.taskStatus")
            )
          )
          .primarySort(PrimarySort.on("_key").ascending(true)));
      LOGGER.info("create ObjectView {}", view);
    }
  }
}
