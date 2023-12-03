package org.hpcclab.oaas.invoker.mq;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.mutiny.core.Vertx;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.model.function.OFunction;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class FunctionListener extends NoGroupKafkaConsumer<OFunction> {

  public FunctionListener() {
  }

  @Inject
  public FunctionListener(Vertx vertx,
                          InvokerConfig config) {
    super(vertx, config, config.fnProvisionTopic());
  }

  @Override
  OFunction deserialize(Buffer buffer) {
    return Json.decodeValue(buffer, OFunction.class);
  }
}
