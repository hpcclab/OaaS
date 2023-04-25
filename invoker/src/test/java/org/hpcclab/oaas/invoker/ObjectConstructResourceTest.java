package org.hpcclab.oaas.invoker;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.json.Json;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.hpcclab.oaas.model.object.ObjectConstructRequest;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.hpcclab.oaas.test.MockupData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@QuarkusTestResource(ArangoResource.class)
public class ObjectConstructResourceTest {
  @Inject
  ObjectRepository objectRepo;
  @Inject
  ClassRepository clsRepo;
  @Inject
  FunctionRepository fnRepo;

  @BeforeEach
  public void setUp() {
    MockupData.persistMock(objectRepo, clsRepo, fnRepo);
  }

  @Test
  public void testSimple() {
    var req = new ObjectConstructRequest()
      .setCls(MockupData.CLS_1.getKey())
      .setStreamConstructs(Lists.fixedSize.of(
        new ObjectConstructRequest()
          .setKeys(Sets.fixedSize.of("k1"))
      ));
    given()
      .when()
      .body(Json.encodePrettily(req))
      .contentType(MediaType.APPLICATION_JSON)
      .post("/api/object-construct")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("object.state.verIds.test", notNullValue());
  }
}
