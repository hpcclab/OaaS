package org.hpcclab.oaas.invoker;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.buffer.Buffer;
import io.vertx.mutiny.kafka.client.producer.KafkaProducer;
import org.hpcclab.oaas.invocation.SyncInvoker;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.hpcclab.oaas.test.MockSyncInvoker;
import org.hpcclab.oaas.test.MockupData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(ArangoResource.class)
public class DeployerTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(DeployerTest.class);

  @Inject
  FunctionRepository funcRepo;
  @Inject
  VerticleDeployer deployer;
  @Inject
  KafkaProducer<String, Buffer> kafkaProducer;
  OaasFunction function;


  @BeforeEach
  void setup() {
    function = MockupData.FUNC_1;
    funcRepo.persistAsync(function).await().indefinitely();
  }


  @Test
  void test() {
    deployer.deployVerticleIfNew(function.getKey())
      .await().indefinitely();
    LOGGER.info("verticleIds {}", deployer.getVerticleIds());
    assertTrue(deployer.getVerticleIds().containsKey(function.getKey()));
    deployer.deleteVerticle(function.getKey())
      .await().indefinitely();
    LOGGER.info("verticleIds {}", deployer.getVerticleIds());
    assertFalse(deployer.getVerticleIds().containsKey(function.getKey()));
  }
}
