package org.hpcclab.oaas.invoker;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.mutiny.kafka.client.producer.KafkaProducer;
import io.vertx.mutiny.kafka.client.producer.KafkaProducerRecord;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.hpcclab.oaas.repository.IdGenerator;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.hpcclab.oaas.test.MockupData;
import org.hpcclab.oaas.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.stream.Stream;

import static org.hpcclab.oaas.test.MockupData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(ArangoResource.class)
class MessageConsumingTest {
  private static final Logger logger = LoggerFactory.getLogger(MessageConsumingTest.class);
  @Inject
  FunctionRepository funcRepo;
  @Inject
  VerticleDeployer deployer;
  @Inject
  KafkaProducer<String, Buffer> kafkaProducer;
  @Inject
  InvokerConfig config;
  @Inject
  ObjectRepository objectRepo;
  @Inject
  ClassRepository clsRepo;
  @Inject
  FunctionRepository fnRepo;
  @Inject
  IdGenerator idGenerator;


  @BeforeEach
  void setup() {
    Stream.of(FUNC_1, MACRO_FUNC_1)
      .forEach(fn -> {
          funcRepo.persistAsync(fn).await().indefinitely();
          deployer.deployVerticleIfNew(fn.getKey())
            .await().indefinitely();
        }
      );
  }


  @Test
  void testSingleMutable() throws InterruptedException {
    MockupData.persistMock(objectRepo, clsRepo, fnRepo);
    var oId = idGenerator.generate();
    var fn = FUNC_1;
    InvocationRequest request = InvocationRequest.builder()
      .target(OBJ_1.getId())
      .outId(oId)
      .fbName("f1")
      .function(fn.getKey())
      .build();
    kafkaProducer.sendAndAwait(KafkaProducerRecord
      .create(config.fnTopicPrefix() + fn.getKey(), request.target(), Json.encodeToBuffer(request))
    );
    TestUtil.retryTillConditionMeet(() -> {
      var o = objectRepo.get(oId);
      return o!=null;
    });
    var main = objectRepo.get(OBJ_1.getId());
    var out = objectRepo.get(oId);
    assertTrue(main.getStatus().getUpdatedOffset() >= 0);
    assertTrue(out.getStatus().getUpdatedOffset() >= 0);
    assertTrue(out.getStatus().getTaskStatus().isCompleted());
    assertEquals(1, main.getData().get("n").asInt());
  }


  @Test
  void testMacro() throws InterruptedException {
    MockupData.persistMock(objectRepo, clsRepo, fnRepo);
    var fn = MACRO_FUNC_1;
    var mid1 = idGenerator.generate();
    var mid2 = idGenerator.generate();
    InvocationRequest request = InvocationRequest.builder()
      .target(OBJ_1.getId())
      .outId(mid2)
      .fbName(fn.getName())
      .function(fn.getKey())
      .macroIds(Map.of(
        "tmp1", mid1,
        "tmp2", mid2
      ))
      .macro(true)
      .build();
    kafkaProducer.sendAndAwait(KafkaProducerRecord
      .create(config.fnTopicPrefix() + fn.getKey(), request.target(), Json.encodeToBuffer(request))
    );
    TestUtil.retryTillConditionMeet(() -> {
      var o = objectRepo.get(mid2);
      return o!= null && o.getStatus().getTaskStatus().isCompleted();
    });

    var m1 = objectRepo.get(mid1);
    assertTrue(m1.getStatus().getUpdatedOffset() >= 0);
    assertTrue(m1.getStatus().getTaskStatus().isCompleted());
    assertEquals(1, m1.getData().get("n").asInt());
    var m2 = objectRepo.get(mid2);
    assertTrue(m1.getStatus().getUpdatedOffset() >= 0);
    assertTrue(m2.getStatus().getTaskStatus().isCompleted());
    assertEquals(2, m2.getData().get("n").asInt());
  }


}
