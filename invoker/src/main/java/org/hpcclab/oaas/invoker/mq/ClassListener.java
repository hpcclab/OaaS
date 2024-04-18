package org.hpcclab.oaas.invoker.mq;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.mutiny.core.Vertx;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.model.cls.OClass;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClassListener extends NoGroupKafkaConsumer<OClass> {

  public ClassListener() {
  }

  @Inject
  public ClassListener(Vertx vertx,
                       InvokerConfig config) {
    super(vertx, config, config.clsProvisionTopic());
  }

  @Override
  OClass deserialize(Buffer buffer) {
    return Json.decodeValue(buffer, OClass.class);
  }
}
