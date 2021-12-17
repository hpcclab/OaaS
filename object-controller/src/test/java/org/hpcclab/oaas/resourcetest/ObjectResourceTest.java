package org.hpcclab.oaas.resourcetest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.vertx.core.json.Json;
import org.hamcrest.Matchers;
import org.hpcclab.oaas.TestUtils;
import org.hpcclab.oaas.model.function.FunctionAccessModifier;
import org.hpcclab.oaas.model.function.FunctionCallRequest;
import org.hpcclab.oaas.model.function.OaasFunctionBindingDto;
import org.hpcclab.oaas.model.object.OaasCompoundMemberDto;
import org.hpcclab.oaas.model.object.OaasObjectDto;
import org.hpcclab.oaas.model.object.ObjectAccessModifier;
import org.hpcclab.oaas.model.proto.OaasObjectPb;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
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
public class ObjectResourceTest {
private static final Logger LOGGER = LoggerFactory.getLogger( ObjectResourceTest.class );
  @Test
  void testCreate() {
    var root = new OaasObjectPb();
    root.setCls("builtin.basic.file");
    root.setState(new OaasObjectState().setBaseUrl("http://test/test.m3u8"));
    root = TestUtils.create(root);
    TestUtils.getObject(root.getId());
    assertTrue(TestUtils.listObject().size() >= 1);
    TestUtils.getObjectDeep(root.getId());
  }

  @Test
  void testFunctionCall() {
    var obj = new OaasObjectPb();
    obj.setCls("builtin.basic.file");
    obj.setState(new OaasObjectState().setBaseUrl("http://test/test.ts"));
    obj = TestUtils.create(obj);

    var newObj = TestUtils.reactiveCall(
      new FunctionCallRequest().setFunctionName("builtin.logical.copy").setTarget(obj.getId()));
    var taskCtx = TestUtils.getTaskContext(newObj.getId());
    Assertions.assertEquals("builtin.logical.copy", taskCtx.getFunction().getName());
  }

  @Test
  void testGetOrigin() {
    TestUtils.createBatchYaml(TestUtils.DUMMY_BATCH);
    var obj = new OaasObjectPb();
    obj.setCls("test.dummy.simple");
    obj.setState(new OaasObjectState().setBaseUrl("http://test/test.ts"));
    obj = TestUtils.create(obj);

    var obj1 = TestUtils.reactiveCall(
      new FunctionCallRequest().setFunctionName("test.dummy.task").setTarget(obj.getId()));
    var obj2 = TestUtils.reactiveCall(
      new FunctionCallRequest().setFunctionName("test.dummy.task").setTarget(obj1.getId()));
    var obj3 = TestUtils.reactiveCall(
      new FunctionCallRequest().setFunctionName("test.dummy.task").setTarget(obj2.getId()));

    given()
      .pathParam("id", obj3.getId().toString())
      .queryParam("deep", 1)
      .when().get("/api/objects/{id}/origin")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("size()", Matchers.equalTo(1))
      .log().ifValidationFails();

    given()
      .pathParam("id", obj3.getId().toString())
      .queryParam("deep", 2)
      .when().get("/api/objects/{id}/origin")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("size()", Matchers.equalTo(2))
      .log().ifValidationFails();

    given()
      .pathParam("id", obj3.getId().toString())
      .queryParam("deep", 5)
      .when().get("/api/objects/{id}/origin")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("size()", Matchers.equalTo(5))
      .log().ifValidationFails();
  }

  @Test
  void testCompound() {
    TestUtils.createBatchYaml(TestUtils.DUMMY_BATCH);
    var obj1 = new OaasObjectPb();
    obj1.setCls("test.dummy.simple");
    obj1.setState(new OaasObjectState().setBaseUrl("http://test/test.ts"));
    obj1 = TestUtils.create(obj1);
    var obj2 = new OaasObjectPb();
    obj2.setCls("test.dummy.simple");
    obj2.setState(new OaasObjectState().setBaseUrl("http://test/test.m3u8"));
    obj2 = TestUtils.create(obj2);
    var compound = new OaasObjectPb();
    compound.setCls("test.dummy.compound");
    compound.setMembers(Set.of(
          new OaasCompoundMemberDto().setName("obj1").setObject(obj1.getId()),
          new OaasCompoundMemberDto().setName("obj2").setObject(obj2.getId())
        )
      );
    compound = TestUtils.create(compound);
    given()
      .pathParam("id", compound.getId().toString())
      .when().get("/api/objects/{id}/deep")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("id", Matchers.equalTo(compound.getId().toString()))
      .body("members.name", hasItems("obj1", "obj2"))
      .body("members.object", hasItems(obj1.getId().toString(), obj2.getId().toString()))
      .log().ifValidationFails();

    var newObj = TestUtils.reactiveCall(
      new FunctionCallRequest().setFunctionName("test.dummy.macro").setTarget(compound.getId()));
  }
}
