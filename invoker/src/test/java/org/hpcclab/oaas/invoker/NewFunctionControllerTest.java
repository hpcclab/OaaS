package org.hpcclab.oaas.invoker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.invocation.controller.fn.logical.NewFunctionController;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.mapper.ProtoMapperImpl;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.object.JsonBytes;
import org.hpcclab.oaas.proto.InvocationService;
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
class NewFunctionControllerTest {
  private static final Logger logger = LoggerFactory.getLogger(NewFunctionControllerTest.class);

  @Inject
  ObjectMapper mapper;
  @Inject
  ClassControllerRegistry registry;
  @Inject
  InvokerManager invokerManager;
  @GrpcClient
  InvocationService invocationService;
  ProtoMapper protoObjectMapper = new ProtoMapperImpl();

  @BeforeEach
  void setup() {
    invokerManager.update(MockupData.CLS_1)
      .await().indefinitely();
    invokerManager.update(MockupData.CLS_2)
      .await().indefinitely();
//    registry.registerOrUpdate(MockupData.CLS_1)
//      .await().indefinitely();
//    registry.registerOrUpdate(MockupData.CLS_2)
//      .await().indefinitely();
  }

  @Test
  void _1createSimple() throws JsonProcessingException {
    var req = NewFunctionController.ObjectConstructRequest.of(
      mapper.createObjectNode().put("n", 1)
    );
    String respString = given()
      .when()
      .body(mapper.valueToTree(req))
      .contentType(ContentType.JSON)
      .post("/api/classes/%s/invokes/new".formatted(MockupData.CLS_1_KEY))
      .then()
      .log().ifValidationFails()
      .statusCode(200)
      .body("output.data.n", Matchers.equalTo(1))
      .extract()
      .body().asString();
    InvocationResponse resp = mapper.readValue(respString, InvocationResponse.class);

    given()
      .when()
      .get("/api/classes/%s/objects/%s".formatted(MockupData.CLS_1_KEY, resp.output().getKey()))
      .then()
      .log().ifValidationFails()
      .statusCode(200)
      .body("data.n", Matchers.equalTo(1));
  }

  @Test
  void _2createSimpleGrpc() {
    var reqBody = NewFunctionController.ObjectConstructRequest.of(
      mapper.createObjectNode().put("n", 1)
    );
    var req = InvocationRequest
      .builder()
      .cls(MockupData.CLS_1_KEY)
      .fb("new")
      .body(new JsonBytes(mapper.valueToTree(reqBody)))
      .build();

    var pRequest = protoObjectMapper.toProto(req);
    var protoInvocationResponse = invocationService.invokeLocal(pRequest)
      .await().indefinitely();
    var resp = protoObjectMapper.fromProto(protoInvocationResponse);
    Assertions.assertThat(resp.output().getData().getNode().get("n").asInt())
      .isEqualTo(1);
  }
}
