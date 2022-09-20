package org.hpcclab.oaas.rest;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hpcclab.oaas.ArangoResource;
import org.hpcclab.oaas.TestUtils;
import org.hpcclab.oaas.controller.service.DataAllocationService;
import org.hpcclab.oaas.model.data.DataAllocateRequest;
import org.hpcclab.oaas.model.data.DataAllocateResponse;
import org.hpcclab.oaas.model.object.ObjectConstructRequest;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(ArangoResource.class)
public class ObjectConstructResourceTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(ObjectConstructResourceTest.class);

  @BeforeEach
  public void setUp() {
    TestUtils.createBatchYaml(TestUtils.DUMMY_BATCH);
    var as = new DataAllocationService(){
      @Override
      public Uni<List<DataAllocateResponse>> allocate(List<DataAllocateRequest> requests) {
        var resList = Lists.fixedSize.ofAll(requests)
          .collect(req -> {
            var m = Lists.fixedSize.ofAll(req.getKeys())
              .toMap(KeySpecification::getName, KeySpecification::getName);
            return new DataAllocateResponse(req.getOid(), m);
          });
        return Uni.createFrom().item(resList);
      }
    };
    QuarkusMock.installMockForType(as, DataAllocationService.class, RestClient.LITERAL);
  }


  @Test
  public void testStream() {
    var req = new ObjectConstructRequest()
      .setCls("test.dummy.stream")
      .setStreamConstructs(Lists.fixedSize.of(
        new ObjectConstructRequest()
          .setKeys(Sets.fixedSize.of("test"))
      ));
    given()
      .when()
      .body(Json.encodePrettily(req))
      .contentType(MediaType.APPLICATION_JSON)
      .post("/api/object-construct")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200);
  }
}
