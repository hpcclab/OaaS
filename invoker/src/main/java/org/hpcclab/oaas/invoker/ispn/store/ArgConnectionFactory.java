package org.hpcclab.oaas.invoker.ispn.store;

import com.arangodb.DbName;
import com.arangodb.async.ArangoCollectionAsync;
import com.arangodb.async.ArangoDBAsync;
import com.arangodb.async.ArangoDatabaseAsync;
import com.arangodb.entity.LoadBalancingStrategy;
import com.arangodb.mapping.ArangoJack;

public class ArgConnectionFactory  implements ConnectionFactory<ArangoCollectionAsync>{
  ArgConnectionConfig connectionConfig;

  public ArgConnectionFactory(ArgConnectionConfig connectionConfig) {
    this.connectionConfig = connectionConfig;
  }

  @Override
  public ArangoCollectionAsync getConnection(String cacheName) {
    return new ArangoDBAsync.Builder()
      .user(connectionConfig.user())
      .password(connectionConfig.pass().orElse(""))
      .host(connectionConfig.host(), connectionConfig.port())
      .maxConnections(30)
      .loadBalancingStrategy(LoadBalancingStrategy.ROUND_ROBIN)
      .acquireHostList(true)
      .serializer(new ArangoJack())
      .build()
      .db(connectionConfig.db())
      .collection(cacheName);
  }
}
