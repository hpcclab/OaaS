package org.hpcclab.oaas.invoker.ispn.store;

import com.arangodb.async.ArangoDBAsync;
import com.arangodb.async.ArangoDatabaseAsync;
import com.arangodb.entity.LoadBalancingStrategy;
import com.arangodb.mapping.ArangoJack;

public class ArgConnectionFactory {
  static ArgConnectionFactory instance;

  ArgConnectionConfig connectionConfig;

  ArgConnectionFactory(ArgConnectionConfig connectionConfig) {
    this.connectionConfig = connectionConfig;
  }

  public ArangoDatabaseAsync getArangoDatabase() {
    return new ArangoDBAsync.Builder()
      .user(connectionConfig.user())
      .password(connectionConfig.pass().orElse(""))
      .host(connectionConfig.host(), connectionConfig.port())
      .maxConnections(30)
      .loadBalancingStrategy(LoadBalancingStrategy.ROUND_ROBIN)
      .acquireHostList(true)
      .serializer(new ArangoJack())
      .build()
      .db();
  }

  public static ArgConnectionFactory getInstance() {
    return instance;
  }

  public static ArgConnectionFactory setConfig(ArgConnectionConfig connectionConfig)  {
    instance = new ArgConnectionFactory(connectionConfig);
    return instance;
  }
}
