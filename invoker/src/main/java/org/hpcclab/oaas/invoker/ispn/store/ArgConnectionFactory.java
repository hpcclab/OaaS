package org.hpcclab.oaas.invoker.ispn.store;

import com.arangodb.DbName;
import com.arangodb.async.ArangoCollectionAsync;
import com.arangodb.async.ArangoDBAsync;
import com.arangodb.async.ArangoDatabaseAsync;
import com.arangodb.entity.LoadBalancingStrategy;
import com.arangodb.mapping.ArangoJack;
import org.hpcclab.oaas.repository.store.DatastoreConf;

import java.util.Optional;

public class ArgConnectionFactory  implements ConnectionFactory<ArangoCollectionAsync>{
  DatastoreConf datastoreConf;

  public ArgConnectionFactory(DatastoreConf datastoreConf) {
    this.datastoreConf = datastoreConf;
  }

  @Override
  public ArangoCollectionAsync getConnection(String cacheName) {
    return new ArangoDBAsync.Builder()
      .user(datastoreConf.user())
      .password(datastoreConf.pass())
      .host(datastoreConf.host(), datastoreConf.port())
      .maxConnections(30)
      .loadBalancingStrategy(LoadBalancingStrategy.ROUND_ROBIN)
      .acquireHostList(true)
      .serializer(new ArangoJack())
      .build()
      .db(datastoreConf.options().getOrDefault("DB", "_system"))
      .collection(datastoreConf.options()
        .getOrDefault("COL", cacheName.replace(".","_")));
  }
}
