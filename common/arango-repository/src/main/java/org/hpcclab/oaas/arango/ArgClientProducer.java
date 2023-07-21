package org.hpcclab.oaas.arango;

import com.arangodb.*;
import com.arangodb.async.ArangoCollectionAsync;
import com.arangodb.async.ArangoDBAsync;
import com.arangodb.async.ArangoDatabaseAsync;
import com.arangodb.entity.LoadBalancingStrategy;
import com.arangodb.mapping.ArangoJack;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
public class ArgClientProducer {

  @Inject
  ArgRepositoryConfig config;

  @Produces
  public ArangoDB arangoDB() {
    return new ArangoDB.Builder()
      .user(config.user())
      .password(config.pass().orElse(""))
      .host(config.host(), config.port())
      .maxConnections(30)
      .loadBalancingStrategy(LoadBalancingStrategy.ROUND_ROBIN)
      .acquireHostList(true)
      .useProtocol(Protocol.VST)
      .serializer(new ArangoJack())
      .build();
  }
  @Produces
  public ArangoDBAsync arangoDBAsync() {
    return new ArangoDBAsync.Builder()
      .user(config.user())
      .password(config.pass().orElse(""))
      .host(config.host(), config.port())
      .maxConnections(30)
      .loadBalancingStrategy(LoadBalancingStrategy.ROUND_ROBIN)
      .acquireHostList(true)
      .serializer(new ArangoJack())
      .build();
  }

  @Produces
  public ArangoDatabase arangoDatabase(ArangoDB arangoDB) {
    return arangoDB.db(config.db());
  }

  @Produces
  public ArangoDatabaseAsync arangoDatabase(ArangoDBAsync arangoDB) {
    return arangoDB.db(config.db());
  }

  @Produces
  @Named("ObjectCollection")
  public ArangoCollection objCol(ArangoDatabase database){
    return database.collection(config.objectCollection());
  }


  @Produces
  @Named("ObjectCollectionAsync")
  public ArangoCollectionAsync objColAsync(ArangoDatabaseAsync database){
    return database.collection(config.objectCollection());
  }


  @Produces
  @Named("FunctionCollection")
  public ArangoCollection funcCol(ArangoDatabase database){
    return database.collection(config.functionCollection());
  }

  @Produces
  @Named("FunctionCollectionAsync")
  public ArangoCollectionAsync funcColAsync(ArangoDatabaseAsync database){
    return database.collection(config.functionCollection());
  }

  @Produces
  @Named("ClassCollection")
  public ArangoCollection clsCol(ArangoDatabase database){
    return database.collection(config.classCollection());
  }

  @Produces
  @Named("ClassCollectionAsync")
  public ArangoCollectionAsync clsColAsync(ArangoDatabaseAsync database){
    return database.collection(config.classCollection());
  }
}
