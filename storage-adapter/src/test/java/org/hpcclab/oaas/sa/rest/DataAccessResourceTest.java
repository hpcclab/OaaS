package org.hpcclab.oaas.sa.rest;

import com.github.f4b6a3.tsid.TsidCreator;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.hamcrest.Matchers;
import org.hpcclab.oaas.model.data.AccessLevel;
import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.object.OObjectType;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.hpcclab.oaas.model.state.StateSpecification;
import org.hpcclab.oaas.model.state.StateType;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.sa.ArangoResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.util.List;

import static io.restassured.RestAssured.given;

@QuarkusTest
@QuarkusTestResource(ArangoResource.class)
class DataAccessResourceTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(DataAccessResourceTest.class);

  @Inject
  ClassRepository clsRepo;

  @BeforeEach
  public void setup() {
    var testCls = new OClass();
    testCls.setName("test");
    testCls.setObjectType(OObjectType.SIMPLE);
    testCls.setStateType(StateType.FILES);
    testCls.setStateSpec(new StateSpecification()
      .setKeySpecs(List.of(
        new KeySpecification("test", "s3")
      ))
    );
    clsRepo.put("test", testCls);
  }

  @Test
  void test() {
    var ctx = new DataAccessContext()
      .setId(TsidCreator.getTsid1024().toString())
      .setVid(TsidCreator.getTsid1024().toString())
      .setCls("test")
      .setLevel(AccessLevel.UNIDENTIFIED);
    var ctxKey = ctx.encode();
    given()
      .pathParam("oid", ctx.getId())
      .pathParam("vid", ctx.getVid())
      .pathParam("key", "test")
      .queryParam("contextKey", ctxKey)
      .when().redirects().follow(false)
      .get("/contents/{oid}/{vid}/{key}")
      .then()
      .log().ifValidationFails()
      .statusCode(Matchers.is(307));
  }
}
