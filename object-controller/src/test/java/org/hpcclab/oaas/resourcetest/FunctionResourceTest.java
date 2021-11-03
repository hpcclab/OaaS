package org.hpcclab.oaas.resourcetest;

import io.quarkus.test.junit.QuarkusTest;
import org.hpcclab.oaas.TestUtils;
import org.hpcclab.oaas.repository.OaasFuncRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItems;

@QuarkusTest
class FunctionResourceTest {
  private static final Logger LOGGER = LoggerFactory.getLogger( FunctionResourceTest.class );
  @Inject
  OaasFuncRepository funcRepo;

  @Test
  void find() {
    var functions = funcRepo.listByNames(
      List.of("builtin.logical.copy")
    ).await().indefinitely();
    Assertions.assertEquals(1, functions.size());
  }


  @Test
  void create() {
    var functionList = TestUtils.createFunctionYaml(TestUtils.DUMMY_FUNCTION);
    given()
      .when().get("/api/functions?update=true")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("name", hasItems("test.dummy.resource","test.dummy.compound"));
  }
}
