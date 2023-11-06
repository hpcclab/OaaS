package org.hpcclab.oaas.arango;

import com.arangodb.*;
import com.arangodb.entity.LoadBalancingStrategy;
import com.arangodb.internal.ArangoDBAsyncImpl;
import com.arangodb.internal.ArangoDBImpl;
import com.arangodb.serde.jackson.JacksonSerde;
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
      .protocol(Protocol.VST)
      .serde(JacksonSerde.of(ContentType.VPACK))
      .build();
  }

  public ArangoDatabase arangoDatabase(ArangoDB arangoDB) {
    return arangoDB.db(conf.options().getOrDefault("DB", "_system"));
  }

  public ArangoDatabaseAsync arangoDatabase(ArangoDBAsync arangoDB) {
    return arangoDB.db(conf.options().getOrDefault("DB", "_system"));
  }

  public ArgClsRepository clsRepository() {
    var db= arangoDB();
    var database = arangoDatabase(db);
    var databaseAsync = arangoDatabase(db.async());
    var cf = new CacheFactory(Integer.parseInt(conf.options().getOrDefault(CACHE_TIMEOUT, "10000")));
    var colName = conf.options().getOrDefault(COLLECTION, "OprcClass");
    return new ArgClsRepository(
      database.collection(colName),
      databaseAsync.collection(colName),
      cf);
  }

  public ArgFunctionRepository fnRepository() {
    var db= arangoDB();
    var database = arangoDatabase(db);
    var databaseAsync = arangoDatabase(db.async());
    var cf = new CacheFactory(Integer.parseInt(conf.options().getOrDefault(CACHE_TIMEOUT, "10000")));
    var colName = conf.options().getOrDefault(COLLECTION, "OprcFunction");
    return new ArgFunctionRepository(
      database.collection(colName),
      databaseAsync.collection(colName),
      cf);
  }

  public ArgObjectRepository objRepository() {
    var db= arangoDB();
    var database = arangoDatabase(db);
    var databaseAsync = arangoDatabase(db.async());
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
