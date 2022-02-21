package org.hpcclab.oaas.storage.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.object.OaasObjectType;
import org.hpcclab.oaas.model.proto.OaasClass;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.model.state.StateSpecification;
import org.hpcclab.oaas.repository.OaasClassRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@QuarkusTest
class DataAccessResourceTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(DataAccessResourceTest.class);

  @BeforeAll
  public static void beforeAll() {
    var testCls = new OaasClass();
    testCls.setName("test");
    testCls.setObjectType(OaasObjectType.SIMPLE);
    testCls.setStateType(OaasObjectState.StateType.FILES);
    testCls.setStateSpec(new StateSpecification()
      .setKeySpecs(List.of(
        new KeySpecification("test", "s3")
      ))
    );
    var mock = Mockito.mock(OaasClassRepository.class);
    Mockito.when(mock.getAsync("test")).thenReturn(Uni.createFrom().item(testCls));
    QuarkusMock.installMockForType(mock, OaasClassRepository.class);
  }

  @Test
  void test() throws JsonProcessingException {
    var ctx = new DataAccessContext()
      .setMainId(UUID.randomUUID())
      .setMainCls("test");
    var ctxString = Json.encode(ctx);
    var ctxKey = Base64.getUrlEncoder().encode(ctxString.getBytes());
    given()
      .pathParam("oid", ctx.getMainId())
      .pathParam("key", "test")
      .queryParam("contextKey", new String(ctxKey))
      .when().redirects().follow(false)
      .get("/contents/{oid}/{key}")
      .then()
      .log().all();
  }
}
