package org.hpcclab.oaas.rest;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.hpcclab.oaas.ArangoResource;
import org.hpcclab.oaas.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItems;

@QuarkusTest
@QuarkusTestResource(ArangoResource.class)
class FunctionResourceTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(FunctionResourceTest.class);

  @Test
  void create() {
    TestUtils.createBatchYaml(TestUtils.DUMMY_BATCH);
    given()
      .when().get("/api/functions")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("items.name", hasItems("builtin.logical.copy",
        "test.dummy.task", "test.dummy.macro"))
      .log().ifValidationFails();
  }
}
