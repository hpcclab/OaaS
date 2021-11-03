package org.hpcclab.oaas.resourcetest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.vertx.core.json.Json;
import org.hamcrest.Matchers;
import org.hpcclab.oaas.TestUtils;
import org.hpcclab.oaas.entity.function.OaasFunctionBinding.AccessModifier;
import org.hpcclab.oaas.entity.object.OaasObject;
import org.hpcclab.oaas.entity.state.OaasObjectState;
import org.hpcclab.oaas.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class ObjectResourceTest {


  @BeforeAll
  static void setup() {
    RestAssured.filters(new RequestLoggingFilter(),
      new ResponseLoggingFilter());
  }

@Test
  void testCreate() {
    var root = new OaasObjectDto()
      .setType(OaasObject.ObjectType.RESOURCE)
      .setCls("builtin.basic.file")
      .setState(new OaasObjectState().setBaseUrl("http://test/test.m3u8"));
    root = TestUtils.create(root);
    TestUtils.getObject(root.getId());
    assertTrue(TestUtils.listObject().size() >=1);
    TestUtils.getObjectDeep(root.getId());

  given()
    .contentType(MediaType.APPLICATION_JSON)
    .when().post("/api/objects")
    .then()
    .contentType(MediaType.APPLICATION_JSON)
    .body("size()", Matchers.greaterThan(0));
  }

  @Test
  void testBind() {
    var obj = new OaasObjectDto()
      .setType(OaasObject.ObjectType.RESOURCE)
      .setCls("builtin.basic.file")
      .setState(new OaasObjectState().setBaseUrl("http://test/test.ts"));
    obj = TestUtils.create(obj);
    var fb = List.of(
      new OaasFunctionBindingDto(AccessModifier.PUBLIC,"builtin.hls.ts.transcode")
    );
    obj = TestUtils.bind(obj, fb);
    var res = TestUtils.getObjectDeep(obj.getId());
    var newFb = res.getFunctions()
      .stream()
      .filter(f -> f.getFunction().getName().equals("builtin.hls.ts.transcode"))
      .findAny();
    assertTrue(newFb.isPresent());
  }


  @Test
  void testBindReject() {
    var obj = new OaasObjectDto()
      .setType(OaasObject.ObjectType.RESOURCE)
      .setAccess(OaasObject.AccessModifier.INTERNAL)
      .setCls("builtin.basic.file")
      .setState(new OaasObjectState().setBaseUrl("http://test/test.ts"));
    obj = TestUtils.create(obj);
    var fb = List.of(
      new OaasFunctionBindingDto(AccessModifier.PUBLIC,
        "builtin.hls.ts.transcode")
    );
    given()
      .contentType(MediaType.APPLICATION_JSON)
      .body(Json.encodePrettily(fb))
      .pathParam("oid", obj.getId().toString())
      .when().post("/api/objects/{oid}/binds")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(400);
    TestUtils.getObjectDeep(obj.getId());
  }


  @Test
  void testFunctionCall() {
    var obj = new OaasObjectDto()
      .setType(OaasObject.ObjectType.RESOURCE)
      .setCls("builtin.basic.file")
      .setState(new OaasObjectState().setBaseUrl("http://test/test.ts"));
    obj = TestUtils.create(obj);

    var newObj = TestUtils.reactiveCall(
      new FunctionCallRequest().setFunctionName("builtin.logical.copy").setTarget(obj.getId()));
    var taskCtx = TestUtils.getTaskContext(newObj.getId());
    Assertions.assertEquals("builtin.logical.copy", taskCtx.getFunction().getName());
  }

  @Test
  void testGetOrigin() {
    TestUtils.createFunctionYaml(TestUtils.DUMMY_FUNCTION);
    var obj = new OaasObjectDto()
      .setType(OaasObject.ObjectType.RESOURCE)
      .setCls("builtin.basic.file")
      .setFunctions(Set.of(
          new OaasFunctionBindingDto().setFunction("test.dummy.resource")
        )
      )
      .setState(new OaasObjectState().setBaseUrl("http://test/test.ts"));
    obj = TestUtils.create(obj);

    var obj1 = TestUtils.reactiveCall(
      new FunctionCallRequest().setFunctionName("test.dummy.resource").setTarget(obj.getId()));
    obj1 = TestUtils.bind(obj1, List.of(new OaasFunctionBindingDto().setFunction("test.dummy.resource")));
    var obj2 = TestUtils.reactiveCall(
      new FunctionCallRequest().setFunctionName("test.dummy.resource").setTarget(obj1.getId()));
    obj2 = TestUtils.bind(obj2, List.of(new OaasFunctionBindingDto().setFunction("test.dummy.resource")));
    var obj3 = TestUtils.reactiveCall(
      new FunctionCallRequest().setFunctionName("test.dummy.resource").setTarget(obj2.getId()));

    given()
      .pathParam("id", obj3.getId().toString())
      .queryParam("deep", 1)
      .when().get("/api/objects/{id}/origin")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("size()", Matchers.equalTo(1));

    given()
      .pathParam("id", obj3.getId().toString())
      .queryParam("deep", 2)
      .when().get("/api/objects/{id}/origin")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("size()", Matchers.equalTo(2));

    given()
      .pathParam("id", obj3.getId().toString())
      .queryParam("deep", 5)
      .when().get("/api/objects/{id}/origin")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("size()", Matchers.equalTo(5));
  }

  @Test
  void testCompound(){
    TestUtils.createFunctionYaml(TestUtils.DUMMY_FUNCTION);
    var obj1 = new OaasObjectDto()
      .setType(OaasObject.ObjectType.RESOURCE)
      .setCls("builtin.basic.file")
      .setState(new OaasObjectState().setBaseUrl("http://test/test.ts"));
    obj1 = TestUtils.create(obj1);
    var obj2 = new OaasObjectDto()
      .setType(OaasObject.ObjectType.RESOURCE)
      .setCls("builtin.basic.file")
      .setState(new OaasObjectState().setBaseUrl("http://test/test.m3u8"));
    obj2 = TestUtils.create(obj2);
    var compound = new OaasObjectDto()
      .setType(OaasObject.ObjectType.COMPOUND)
      .setCls("builtin.basic.compound")
      .setMembers(Set.of(
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
      .body("members.name", hasItems("obj1","obj2"))
      .body("members.object", hasItems(obj1.getId().toString(),obj2.getId().toString()));

    compound = TestUtils.bind(compound, List.of(
      new OaasFunctionBindingDto().setFunction("test.dummy.compound")
    ));

    var newObj = TestUtils.reactiveCall(
      new FunctionCallRequest().setFunctionName("test.dummy.compound").setTarget(compound.getId()));
  }

}
