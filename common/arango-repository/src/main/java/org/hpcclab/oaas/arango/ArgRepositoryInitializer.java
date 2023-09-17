package org.hpcclab.oaas.arango;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.ArangoView;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.model.PersistentIndexOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  @Named("FunctionCollection")
  ArangoCollection funcCol;
  @Inject
  @Named("ClassCollection")
  ArangoCollection clsCol;

  public void setup() {
    if (!database.exists()) database.create();
    if (!objCol.exists()) {
      objCol.create(new CollectionCreateOptions());
//      objCol.ensurePersistentIndex(List.of("cls"), new PersistentIndexOptions());
    }

    if (!funcCol.exists()) {
      funcCol.create();
    }
    if (!clsCol.exists()) {
      clsCol.create();
    }
  }
}
