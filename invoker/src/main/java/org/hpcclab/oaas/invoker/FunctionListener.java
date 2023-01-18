package org.hpcclab.oaas.invoker;

import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.kafka.client.common.KafkaClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.kafka.admin.KafkaAdminClient;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumer;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.hpcclab.oaas.model.function.OaasFunction;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;
import java.util.function.Consumer;

@ApplicationScoped
public class FunctionListener {
  @Inject
  Vertx vertx;
  @Inject
  KafkaClientOptions options;
  KafkaConsumer<String, Buffer> client;
  KafkaAdminClient adminClient;

  String groupId;

  @PostConstruct
  void setup() {
    var config = options.getConfig();
    var modOption = new KafkaClientOptions();
    var newConfig = new HashMap<String, Object>(config);
    var rand = new Random();
    groupId = "oaas-invoker-%04d".formatted(rand.nextInt(10000));
    newConfig.put("group.id", groupId);
    newConfig.put("auto.offset.reset", "latest");
    client = KafkaConsumer.create(
      vertx, modOption.setConfig(newConfig), String.class, Buffer.class
    );
    adminClient = KafkaAdminClient.create(vertx, Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, (String) config.get(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG)));
  }

  @PreDestroy
  void clean() {
    client.close()
      .call(() -> adminClient.deleteConsumerGroups(List.of(groupId)))
      .await().indefinitely();
  }

  public Uni<Void> start() {
    return client.subscribe("oaas-provisions");
  }

  public void setHandler(Consumer<OaasFunction> consumer) {
    client.handler(record -> {
      var fn = Json.decodeValue(record.value(), OaasFunction.class);
      consumer.accept(fn);
    });
  }
}
