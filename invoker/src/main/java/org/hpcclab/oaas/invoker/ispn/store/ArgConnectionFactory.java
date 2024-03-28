package org.hpcclab.oaas.invoker.ispn.store;


import com.arangodb.ArangoCollectionAsync;
import com.arangodb.ArangoDB;
import com.arangodb.ContentType;
import com.arangodb.Protocol;
import com.arangodb.entity.LoadBalancingStrategy;
import com.arangodb.serde.ArangoSerde;
import com.arangodb.serde.jackson.JacksonSerde;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.quarkus.vertx.runtime.jackson.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.hpcclab.oaas.model.OprcJsonUtil;
import org.hpcclab.oaas.repository.store.DatastoreConf;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

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
