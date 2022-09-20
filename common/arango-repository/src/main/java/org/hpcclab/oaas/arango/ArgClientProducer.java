package org.hpcclab.oaas.arango;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.DbName;
import com.arangodb.async.ArangoCollectionAsync;
import com.arangodb.async.ArangoDBAsync;
import com.arangodb.async.ArangoDatabaseAsync;
import com.arangodb.mapping.ArangoJack;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

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
      .maxConnections(16)
      .serializer(new ArangoJack())
      .build();
  }
  @Produces
  public ArangoDBAsync arangoDBAsync() {
    return new ArangoDBAsync.Builder()
      .user(config.user())
      .password(config.pass().orElse(""))
      .host(config.host(), config.port())
      .maxConnections(16)
      .serializer(new ArangoJack())
      .build();
  }

  @Produces
  public ArangoDatabase arangoDatabase(ArangoDB arangoDB) {
    return arangoDB.db(DbName.of(config.db()));
  }

  @Produces
  public ArangoDatabaseAsync arangoDatabase(ArangoDBAsync arangoDB) {
    return arangoDB.db(DbName.of(config.db()));
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

  @Produces
  @Named("OdeCollectionAsync")
  public ArangoCollectionAsync odeColAsync(ArangoDatabaseAsync database){
    return database.collection(config.odeCollection());
  }
}
