package org.hpcclab.oaas.invoker;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.mutiny.kafka.client.producer.KafkaProducer;
import io.vertx.mutiny.kafka.client.producer.KafkaProducerRecord;
import org.hpcclab.oaas.model.function.FunctionState;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.hpcclab.oaas.test.MockupData;
import org.hpcclab.oaas.test.TestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(ArangoResource.class)
class DeployerTest {
  private static final Logger logger = LoggerFactory.getLogger(DeployerTest.class);

  @Inject
  FunctionRepository funcRepo;
  @Inject
  VerticleDeployer deployer;
  @Inject
  KafkaProducer<String, Buffer> kafkaProducer;
  @Inject
  InvokerConfig config;

//  @Test
  void testManualDeploy() {
    var function = MockupData.FUNC_2;
    funcRepo.persistAsync(function).await().indefinitely();
    deployer.deployVerticleIfNew(function.getKey())
      .await().indefinitely();
    logger.info("verticleIds {}", deployer.getVerticleIds());
    assertTrue(deployer.getVerticleIds().containsKey(function.getKey()));
    deployer.deleteVerticle(function.getKey())
      .await().indefinitely();
    logger.info("verticleIds {}", deployer.getVerticleIds());
    assertFalse(deployer.getVerticleIds().containsKey(function.getKey()));
  }

//  @Test
  void testSubscribingDeploy() throws InterruptedException {
    var function = MockupData.FUNC_2;
    funcRepo.persistAsync(function).await().indefinitely();
    kafkaProducer.sendAndAwait(
      KafkaProducerRecord.create(
        config.fnProvisionTopic(),
        function.getKey(),
        Json.encodeToBuffer(function)
        )
    );
    assertTrue(TestUtil.retryTillConditionMeet(() ->
      deployer.getVerticleIds().containsKey(function.getKey()))
    );
    var fn = function.copy();
    fn.setState(FunctionState.REMOVING);
    kafkaProducer.sendAndAwait(
      KafkaProducerRecord.create(
        config.fnProvisionTopic(),
        function.getKey(),
        Json.encodeToBuffer(fn)
      )
    );
    assertTrue(TestUtil.retryTillConditionMeet(() ->
      !deployer.getVerticleIds().containsKey(function.getKey()))
    );
  }
}
