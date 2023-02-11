package org.hpcclab.oaas.invoker;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.buffer.Buffer;
import io.vertx.mutiny.kafka.client.producer.KafkaProducer;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.hpcclab.oaas.test.MockupData;
import org.junit.jupiter.api.BeforeEach;

import javax.inject.Inject;

@QuarkusTest
@QuarkusTestResource(ArangoResource.class)
public class MessageConsumingTest {

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
    deployer.deployVerticleIfNew(function.getKey())
      .await().indefinitely();
  }


//  @Test
//  void test() {
//  }
}
