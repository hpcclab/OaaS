package org.hpcclab.oaas.invoker.ispn.store;


import com.arangodb.ArangoCollectionAsync;
import com.arangodb.ArangoDB;
import com.arangodb.ContentType;
import com.arangodb.Protocol;
import com.arangodb.entity.LoadBalancingStrategy;
import com.arangodb.serde.jackson.JacksonSerde;
import org.hpcclab.oaas.model.OprcJsonUtil;
import org.hpcclab.oaas.repository.store.DatastoreConf;

public class ArgConnectionFactory  implements ConnectionFactory<ArangoCollectionAsync>{
  DatastoreConf datastoreConf;

  public ArgConnectionFactory(DatastoreConf datastoreConf) {
    this.datastoreConf = datastoreConf;
  }

  @Override
  public ArangoCollectionAsync getConnection(String cacheName) {
    return new ArangoDB.Builder()
      .user(datastoreConf.user())
      .password(datastoreConf.pass())
      .host(datastoreConf.host(), datastoreConf.port())
      .maxConnections(30)
      .loadBalancingStrategy(LoadBalancingStrategy.ROUND_ROBIN)
      .acquireHostList(true)
      .protocol(Protocol.VST)
      .serde(JacksonSerde.of(ContentType.VPACK)
        .configure(mapper -> mapper.registerModule(OprcJsonUtil.createModule())))
      .build()
      .async()
      .db(datastoreConf.options().getOrDefault("DB", "_system"))
      .collection(datastoreConf.options()
        .getOrDefault("COL", cacheName.replace(".","_")));
  }
}
