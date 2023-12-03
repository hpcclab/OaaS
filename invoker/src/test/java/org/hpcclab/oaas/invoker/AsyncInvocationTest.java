package org.hpcclab.oaas.invoker;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.mutiny.kafka.client.producer.KafkaProducer;
import io.vertx.mutiny.kafka.client.producer.KafkaProducerRecord;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.hpcclab.oaas.repository.ObjectRepoManager;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.hpcclab.oaas.test.MockupData;
import org.hpcclab.oaas.test.TestUtil;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hpcclab.oaas.test.MockupData.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(ArangoResource.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AsyncInvocationTest {
  private static final Logger logger = LoggerFactory.getLogger(AsyncInvocationTest.class);
  @Inject
  VerticleDeployer deployer;
  @Inject
  KafkaProducer<String, Buffer> kafkaProducer;
  @Inject
  InvokerConfig config;
  @Inject
  ObjectRepoManager objectRepoManager;
  @Inject
  ClassRepository clsRepo;
  @Inject
  FunctionRepository fnRepo;
  @Inject
  IdGenerator idGenerator;
  List<OClass> clsList = List.of(
    CLS_1
  );

  @BeforeEach
  void setup() {
    for (var cls : clsList) {
      clsRepo.put(cls.getKey(), cls);
      deployer.createTopic(cls)
          .await().indefinitely();
      deployer.deployVerticleIfNew(cls)
        .await().indefinitely();
    }
  }


  @Test
  void _0testSubscribingDeploy() throws InterruptedException {
    var cls = CLS_2;
    MockupData.persistMock(objectRepoManager, clsRepo, fnRepo);
    kafkaProducer.sendAndAwait(
      KafkaProducerRecord.create(
        config.clsProvisionTopic(),
        cls.getKey(),
        Json.encodeToBuffer(cls)
      )
    );
    assertTrue(TestUtil.retryTillConditionMeet(() ->
      deployer.getVerticleIds().containsKey(cls.getKey()))
    );
    var newCls = cls.copy();
    newCls.setMarkForRemoval(true);
    kafkaProducer.sendAndAwait(
      KafkaProducerRecord.create(
        config.clsProvisionTopic(),
        newCls.getKey(),
        Json.encodeToBuffer(newCls)
      )
    );
    assertTrue(TestUtil.retryTillConditionMeet(() ->
      !deployer.getVerticleIds().containsKey(newCls.getKey()))
    );
  }

  @Test
  void _1testSingleMutable() throws InterruptedException {
    MockupData.persistMock(objectRepoManager, clsRepo, fnRepo);
    var main = OBJ_1.copy();
    main.setId(idGenerator.generate());
    var objectRepo = objectRepoManager.getOrCreate(CLS_1);
    objectRepo.persist(main);
    var oId = idGenerator.generate();
    var fn = FUNC_1;
    var cls = CLS_1;
    InvocationRequest request = InvocationRequest.builder()
      .cls(cls.getKey())
      .main(main.getId())
      .outId(oId)
      .fb("f1")
      .invId(idGenerator.generate())
      .build();
    kafkaProducer.sendAndAwait(KafkaProducerRecord
      .create(config.invokeTopicPrefix() + cls.getKey(), request.main(), Json.encodeToBuffer(request))
    );
    TestUtil.retryTillConditionMeet(() -> {
      var o = objectRepo.get(oId);
      return o!=null;
    });
    main = objectRepo.get(main.getId());
    var out = objectRepo.get(oId);
    assertThat(main.getLastOffset())
      .withFailMessage("Offset is negative %s", main)
      .isNotNegative();
    assertThat(out.getLastOffset())
      .isNegative();
    assertThat(main.getData().get("n").asInt())
      .isEqualTo(1);
  }


  @Test
  void _2testMacro() throws InterruptedException {
    MockupData.persistMock(objectRepoManager, clsRepo, fnRepo);
    var main = OBJ_1.copy();
    main.setId(idGenerator.generate());
    var objectRepo = objectRepoManager.getOrCreate(CLS_1);
    objectRepo.put(main.getKey(), main);
    var fn = MACRO_FUNC_1;
    var cls = CLS_1;

    var mid1 = idGenerator.generate();
    var mid2 = idGenerator.generate();
    var mid3 = idGenerator.generate();
    InvocationRequest request = InvocationRequest.builder()
      .cls(cls.getKey())
      .main(main.getId())
      .outId(mid3)
      .fb(fn.getName())
      .macroIds(DSMap.of(
        "tmp1", mid1,
        "tmp2", mid2,
        "tmp3", mid3
      ))
      .macro(true)
      .invId(idGenerator.generate())
      .build();
    kafkaProducer.sendAndAwait(KafkaProducerRecord
      .create(config.invokeTopicPrefix() + cls.getKey(), request.main(), Json.encodeToBuffer(request))
    );
    TestUtil.retryTillConditionMeet(() -> objectRepo.get(mid1)!= null);

    var m1 = objectRepo.get(mid1);
    assertNotNull(m1);
    assertThat(m1.getLastOffset() )
      .isNegative();
    assertEquals(1, m1.getData().get("n").asInt());

    TestUtil.retryTillConditionMeet(() -> objectRepo.get(mid3) != null);

    var m3 = objectRepo.get(mid3);
    assertNotNull(m3);
    assertThat(m3.getLastOffset())
      .isNegative();
    assertThat(m3.getData().get("n").asInt())
      .isEqualTo(3);
  }


  @Test
  void _3testAtomicMacro() throws InterruptedException {
    MockupData.persistMock(objectRepoManager, clsRepo, fnRepo);
    var main = OBJ_1.copy();
    main.setId(idGenerator.generate());
    var objectRepo = objectRepoManager.getOrCreate(CLS_1);
    objectRepo.put(main.getKey(), main);
    var fn = ATOMIC_MACRO_FUNC;
    var cls = CLS_1;

    var mid1 = idGenerator.generate();
    var mid2 = idGenerator.generate();
    var mid3 = idGenerator.generate();
    InvocationRequest request = InvocationRequest.builder()
      .cls(cls.getKey())
      .main(main.getId())
      .outId(mid2)
      .fb(fn.getName())
      .macroIds(DSMap.of(
        "tmp1", mid1,
        "tmp2", mid2,
        "tmp3", mid3
      ))
      .macro(true)
      .invId(idGenerator.generate())
      .build();
    kafkaProducer.sendAndAwait(KafkaProducerRecord
      .create(config.invokeTopicPrefix() + cls.getKey(), request.main(), Json.encodeToBuffer(request))
    );

    TestUtil.retryTillConditionMeet(() -> {
      var o = objectRepo.get(mid3);
      return o!= null;
    });

    var m1 = objectRepo.get(mid1);
    assertNotNull(m1);
    assertThat(m1.getData().get("n").asInt())
      .withFailMessage("n should be 1 %s", m1)
      .isEqualTo(1);
    var m2 = objectRepo.get(mid2);
    assertNotNull(m2);
    assertThat(m2.getData().get("n").asInt())
      .withFailMessage("n should be 1 %s", m2)
      .isEqualTo(1);
    var m3 = objectRepo.get(mid3);
    assertNotNull(m3);
    assertThat(m3.getLastOffset())
      .isNegative();
    assertThat(m3.getData().get("n").asInt())
      .withFailMessage("n should be 3 %s", m3)
      .isEqualTo(3);
  }


}
