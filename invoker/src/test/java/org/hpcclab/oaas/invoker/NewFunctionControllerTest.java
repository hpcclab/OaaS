package org.hpcclab.oaas.invoker;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.hpcclab.oaas.invocation.InvocationManager;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.invocation.controller.fn.logical.NewFunctionController;
import org.hpcclab.oaas.mapper.ProtoObjectMapper;
import org.hpcclab.oaas.mapper.ProtoObjectMapperImpl;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.proto.InvocationService;
import org.hpcclab.oaas.test.MockupData;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runners.MethodSorters;
import org.msgpack.jackson.dataformat.MessagePackMapper;
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
  ProtoObjectMapper protoObjectMapper = new ProtoObjectMapperImpl();

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
    protoObjectMapper.setMapper(new MessagePackMapper());
  }

  @Test
  void _1createSimple() {
    var req = NewFunctionController.ObjectConstructRequest.of(
      mapper.createObjectNode().put("n", 1)
    );
    given()
      .when()
      .body( mapper.valueToTree(req))
      .contentType(ContentType.JSON)
      .post("/api/classes/%s/invokes/new".formatted(MockupData.CLS_1_KEY))
      .then()
      .log().ifValidationFails()
      .statusCode(200)
      .body("output.data.n", Matchers.equalTo(1))
    ;
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
      .body(mapper.valueToTree(reqBody))
      .build();

    var pRequest = protoObjectMapper.toProto(req);
    var protoInvocationResponse = invocationService.invokeLocal(pRequest)
      .await().indefinitely();
    var resp = protoObjectMapper.fromProto(protoInvocationResponse);
    Assertions.assertThat(resp.output().getData().get("n").asInt())
      .isEqualTo(1);
  }
}
