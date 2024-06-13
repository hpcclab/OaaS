package org.hpcclab.oaas.invoker;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.hamcrest.Matchers;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.model.object.GOObject;
import org.hpcclab.oaas.model.object.OMeta;
import org.hpcclab.oaas.repository.ObjectRepoManager;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.hpcclab.oaas.test.MockupData;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.restassured.RestAssured.given;
import static org.hpcclab.oaas.test.MockupData.CLS_1_KEY;

@QuarkusTest
@QuarkusTestResource(ArangoResource.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SyncInvocationTest {
  private static final Logger logger = LoggerFactory.getLogger(SyncInvocationTest.class);

  @Inject
  ObjectRepoManager objectRepoManager;
  @Inject
  IdGenerator idGenerator;
  @Inject
  ClassControllerRegistry registry;
  @Inject
  InvokerManager invokerManager;

  @BeforeEach
  void setup() {
    invokerManager.update(MockupData.CLS_1)
      .await().indefinitely();
    invokerManager.update(MockupData.CLS_2)
      .await().indefinitely();
  }

  @Test
  void _1testSingle() {
    var main = new GOObject();
    OMeta meta = main.getMeta();
    meta.setId(idGenerator.generate());
    meta.setCls(CLS_1_KEY);
    objectRepoManager.persistAsync(main).await().indefinitely();
    given()
      .when()
      .queryParam("_showAll", "true")
      .get("/api/classes/{cls}/objects/{oid}/invokes/{fb}",
        meta.getCls(), meta.getId(), "f1")
      .then()
      .log().ifValidationFails()
      .statusCode(200)
      .body("main.data.n", Matchers.equalTo(1));
    given()
      .when()
      .queryParam("_showAll", "true")
      .get("/api/classes/{cls}/objects/{oid}/invokes/{fb}",
        meta.getCls(), meta.getId(), "f1")
      .then()
      .log().ifValidationFails()
      .statusCode(200)
      .body("main.data.n", Matchers.equalTo(2));
  }
}
