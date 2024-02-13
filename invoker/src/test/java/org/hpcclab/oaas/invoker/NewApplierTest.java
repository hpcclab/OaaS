package org.hpcclab.oaas.invoker;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.hamcrest.Matchers;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.invocation.applier.logical.ObjectConstructRequest;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.hpcclab.oaas.repository.ObjectRepoManager;
import org.hpcclab.oaas.test.MockupData;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.restassured.RestAssured.given;

@QuarkusTest
@QuarkusTestResource(ArangoResource.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NewApplierTest {
  private static final Logger logger = LoggerFactory.getLogger( NewApplierTest.class );

  @Inject
  ObjectMapper mapper;
  @Inject
  ClassControllerRegistry registry;

  @BeforeEach
  void setup() {
    registry.registerOrUpdate(MockupData.CLS_1)
      .await().indefinitely();
    registry.registerOrUpdate(MockupData.CLS_2)
      .await().indefinitely();
  }

  @Test
  public void _1createSimple() {
    var req = new ObjectConstructRequest()
      .setData(mapper.createObjectNode().put("n", 1))
      ;

    var oal = new ObjectAccessLanguage(
      null,
      MockupData.CLS_1_KEY,
      "new",
      mapper.valueToTree(req),
      null,
      null
    );
    logger.info("oal {}", oal);
    given()
      .when()
      .body(oal)
      .contentType(ContentType.JSON)
      .post("/oal")
      .then()
      .log().ifValidationFails()
      .statusCode(200)
      .body("output.data.n", Matchers.equalTo(1))
      ;
  }
}
