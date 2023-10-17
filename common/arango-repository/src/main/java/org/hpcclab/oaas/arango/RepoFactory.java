package org.hpcclab.oaas.arango;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.Protocol;
import com.arangodb.async.ArangoDBAsync;
import com.arangodb.async.ArangoDatabaseAsync;
import com.arangodb.entity.LoadBalancingStrategy;
import com.arangodb.mapping.ArangoJack;
import org.hpcclab.oaas.arango.repo.ArgClsRepository;
import org.hpcclab.oaas.arango.repo.ArgFunctionRepository;
import org.hpcclab.oaas.arango.repo.ArgObjectRepository;
import org.hpcclab.oaas.repository.store.DatastoreConf;

public class RepoFactory {
  static final String CACHE_TIMEOUT = "CACHETIMEOUT";
  static final String COLLECTION = "COL";
  DatastoreConf conf;

  public RepoFactory(DatastoreConf conf) {
    this.conf = conf;
  }

  public ArangoDB arangoDB() {
    return new ArangoDB.Builder()
      .user(conf.user())
      .password(conf.pass()!=null ? conf.pass():"")
      .host(conf.host(), conf.port())
      .maxConnections(30)
      .loadBalancingStrategy(LoadBalancingStrategy.ROUND_ROBIN)
      .acquireHostList(true)
      .useProtocol(Protocol.VST)
      .serializer(new ArangoJack())
      .build();
  }

  public ArangoDBAsync arangoDBAsync() {
    return new ArangoDBAsync.Builder()
      .user(conf.user())
      .password(conf.pass()!=null ? conf.pass():"")
      .host(conf.host(), conf.port())
      .maxConnections(30)
      .loadBalancingStrategy(LoadBalancingStrategy.ROUND_ROBIN)
      .acquireHostList(true)
      .serializer(new ArangoJack())
      .build();
  }

  public ArangoDatabase arangoDatabase(ArangoDB arangoDB) {
    return arangoDB.db(conf.options().getOrDefault("DB", "_system"));
  }

  public ArangoDatabaseAsync arangoDatabase(ArangoDBAsync arangoDB) {
    return arangoDB.db(conf.options().getOrDefault("DB", "_system"));
  }

  public ArgClsRepository clsRepository() {
    var database = arangoDatabase(arangoDB());
    var databaseAsync = arangoDatabase(arangoDBAsync());
    var cf = new CacheFactory(Integer.parseInt(conf.options().getOrDefault(CACHE_TIMEOUT, "10000")));
    var colName = conf.options().getOrDefault(COLLECTION, "OprcClass");
    return new ArgClsRepository(
      database.collection(colName),
      databaseAsync.collection(colName),
      cf);
  }

  public ArgFunctionRepository fnRepository() {
    var database = arangoDatabase(arangoDB());
    var databaseAsync = arangoDatabase(arangoDBAsync());
    var cf = new CacheFactory(Integer.parseInt(conf.options().getOrDefault(CACHE_TIMEOUT, "10000")));
    var colName = conf.options().getOrDefault(COLLECTION, "OprcFunction");
    return new ArgFunctionRepository(
      database.collection(colName),
      databaseAsync.collection(colName),
      cf);
  }

  public ArgObjectRepository objRepository() {
    var database = arangoDatabase(arangoDB());
    var databaseAsync = arangoDatabase(arangoDBAsync());
    var colName = conf.options().getOrDefault(COLLECTION, "OprcObject");
    return new ArgObjectRepository(
      database.collection(colName),
      databaseAsync.collection(colName));
  }

  public void init() {
    var database = arangoDatabase(arangoDB());
    if (!database.exists()) {
      database.create();
    }
    var col = database.collection(conf.options().getOrDefault(COLLECTION, "OprcClass"));
    if (!col.exists()) {
      col.create();
    }
    col = database.collection(conf.options().getOrDefault(COLLECTION, "OprcFunction"));
    if (!col.exists()) {
      col.create();
    }
    col = database.collection(conf.options().getOrDefault(COLLECTION, "OprcObject"));
    if (!col.exists()) {
      col.create();
    }
  }
}
