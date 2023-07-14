package org.hpcclab.oaas.invoker;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.mutiny.kafka.client.producer.KafkaProducer;
import io.vertx.mutiny.kafka.client.producer.KafkaProducerRecord;
import jakarta.inject.Inject;
import org.hamcrest.Matchers;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.hpcclab.oaas.test.MockupData;
import org.hpcclab.oaas.test.TestUtil;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hpcclab.oaas.test.MockupData.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(ArangoResource.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SyncInvocationTest {
  private static final Logger logger = LoggerFactory.getLogger(SyncInvocationTest.class);

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
    MockupData.persistMock(objectRepo, clsRepo, fnRepo);
  }

  @Test
  void _1testSingleMutable() {
    var main = OBJ_1.copy();
    main.setId(idGenerator.generate());
    objectRepo.put(main.getKey(), main);
    ObjectAccessLanguage oal = ObjectAccessLanguage.parse("%s:%s".formatted(main.getId(), "f1"));
    given()
      .when()
      .get("/oal/{oal}", oal.toString())
      .then()
      .log().all()
      .statusCode(200)
      .body("main.data.n", Matchers.equalTo(1));
    given()
      .when()
      .get("/oal/{oal}", oal.toString())
      .then()
      .log().all()
      .statusCode(200)
      .body("main.data.n", Matchers.equalTo(2));
  }
}
