package org.hpcclab.oaas.arango;

import com.arangodb.*;
import com.arangodb.entity.LoadBalancingStrategy;
import com.arangodb.serde.jackson.JacksonSerde;
import org.hpcclab.oaas.arango.repo.ArgClsRepository;
import org.hpcclab.oaas.arango.repo.ArgFunctionRepository;
import org.hpcclab.oaas.arango.repo.GenericArgRepository;
import org.hpcclab.oaas.repository.store.DatastoreConf;

import java.util.function.Function;

public class RepoFactory {
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
    var db = arangoDB();
    var database = arangoDatabase(db);
    var databaseAsync = arangoDatabase(db.async());
    var colName = conf.options().getOrDefault(COLLECTION, "OprcClass");
    return new ArgClsRepository(
      database.collection(colName),
      databaseAsync.collection(colName));
  }

  public ArgFunctionRepository fnRepository() {
    var db = arangoDB();
    var database = arangoDatabase(db);
    var databaseAsync = arangoDatabase(db.async());
    var colName = conf.options().getOrDefault(COLLECTION, "OprcFunction");
    return new ArgFunctionRepository(
      database.collection(colName),
      databaseAsync.collection(colName));
  }

  public <V> GenericArgRepository<V> createGenericRepo(Class<V> cls,
                                                       Function<V, String> keyExtractor,
                                                       String defaultCollection) {
    var db = arangoDB();
    var database = arangoDatabase(db);
    var databaseAsync = arangoDatabase(db.async());
    var colName = conf.options().getOrDefault(COLLECTION, defaultCollection);
    return new GenericArgRepository<>(
      cls,
      keyExtractor,
      database.collection(colName),
      databaseAsync.collection(colName)
    );
  }

  public void init() {
    var database = arangoDatabase(arangoDB());
    if (!database.exists()) {
      database.create();
    }
    createIfNotExist(database, conf.options().getOrDefault("CLS_COL", "OprcClass"));
    createIfNotExist(database, conf.options().getOrDefault("FN_COL", "OprcFunction"));
  }

  public static void createIfNotExist(ArangoDatabase database, String colName) {
    var col = database.collection(colName);
    if (!col.exists()) {
      col.create();
    }
  }
}
