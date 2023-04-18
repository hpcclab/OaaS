package org.hpcclab.oaas.invoker;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.kafka.client.common.KafkaClientOptions;
import io.vertx.mutiny.core.Vertx;
import org.hpcclab.oaas.model.function.OaasFunction;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class FunctionListener extends NoGroupKafkaConsumer<OaasFunction> {

  public FunctionListener() {
  }

  @Inject
  public FunctionListener(Vertx vertx,
                          KafkaClientOptions options,
                          InvokerConfig config) {
    super(vertx, options, config.fnProvisionTopic());
  }

  @Override
  OaasFunction deserialize(Buffer buffer) {
    return Json.decodeValue(buffer, OaasFunction.class);
  }
}
