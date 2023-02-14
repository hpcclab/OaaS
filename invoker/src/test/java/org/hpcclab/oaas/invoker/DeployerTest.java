package org.hpcclab.oaas.invoker;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.kafka.admin.NewTopic;
import io.vertx.kafka.client.common.KafkaClientOptions;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.kafka.admin.KafkaAdminClient;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumer;
import io.vertx.mutiny.kafka.client.producer.KafkaProducer;
import io.vertx.mutiny.kafka.client.producer.KafkaProducerRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.hpcclab.oaas.test.MockupData;
import org.hpcclab.oaas.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(ArangoResource.class)
class DeployerTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(DeployerTest.class);

  @Inject
  FunctionRepository funcRepo;
  @Inject
  VerticleDeployer deployer;
  @Inject
  KafkaProducer<String, Buffer> kafkaProducer;
  OaasFunction function;
  @Inject
  InvokerConfig config;


  @BeforeEach
  void setup() {
    function = MockupData.FUNC_1;
    funcRepo.persistAsync(function).await().indefinitely();
  }


  @Test
  void testManualDeploy() {
    deployer.deployVerticleIfNew(function.getKey())
      .await().indefinitely();
    LOGGER.info("verticleIds {}", deployer.getVerticleIds());
    assertTrue(deployer.getVerticleIds().containsKey(function.getKey()));
    deployer.deleteVerticle(function.getKey())
      .await().indefinitely();
    LOGGER.info("verticleIds {}", deployer.getVerticleIds());
    assertFalse(deployer.getVerticleIds().containsKey(function.getKey()));
  }

  @Test
  void testSubscribingDeploy() throws InterruptedException {
    kafkaProducer.sendAndAwait(
      KafkaProducerRecord.create(
        config.fnProvisionTopic(),
        MockupData.FUNC_1.getKey(),
        Json.encodeToBuffer(MockupData.FUNC_1)
        )
    );
    assertTrue(TestUtil.retryTillConditionMeet(() ->
      deployer.getVerticleIds().containsKey(function.getKey()))
    );
  }
}
