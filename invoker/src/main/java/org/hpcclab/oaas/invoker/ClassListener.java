package org.hpcclab.oaas.invoker;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.kafka.client.common.KafkaClientOptions;
import io.vertx.mutiny.core.Vertx;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.function.OaasFunction;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ClassListener extends NoGroupKafkaConsumer<OaasClass> {

  public ClassListener() {
  }

  @Inject
  public ClassListener(Vertx vertx,
                       KafkaClientOptions options,
                       InvokerConfig config) {
    super(vertx, options, config.clsProvisionTopic());
  }

  @Override
  OaasClass deserialize(Buffer buffer) {
    return Json.decodeValue(buffer, OaasClass.class);
  }
}
