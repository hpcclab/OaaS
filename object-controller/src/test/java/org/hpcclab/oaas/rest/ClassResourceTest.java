package org.hpcclab.oaas.rest;

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
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("items.name", hasItems("test.dummy.simple", "test.dummy.compound"));
    given()
      .when().get("/api/classes/test.dummy.simple")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200);
  }

//  @Test
//  void testListObject() {
//    TestUtils.createBatchYaml(TestUtils.DUMMY_BATCH);
//  }
}
