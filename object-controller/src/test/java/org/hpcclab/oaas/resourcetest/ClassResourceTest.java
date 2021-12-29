package org.hpcclab.oaas.resourcetest;

import io.quarkus.test.junit.QuarkusTest;
import org.hpcclab.oaas.TestUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItems;

@QuarkusTest
class ClassResourceTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClassResourceTest.class);
  @Test
  void create() {
    TestUtils.createBatchYaml(TestUtils.DUMMY_BATCH);
    given()
      .when().get("/api/classes")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("name", hasItems("test.dummy.simple", "test.dummy.compound"))
      .log().ifValidationFails();
    given()
      .when().get("/api/classes/test.dummy.simple")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .log().ifValidationFails();
    given()
      .when().get("/api/classes/test.dummy.simple/deep")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .log().all();
  }
}
