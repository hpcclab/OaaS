package org.hpcclab.msc.resourcetest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.vertx.core.json.Json;
import org.hpcclab.msc.TestUtils;
import org.hpcclab.msc.object.entity.function.OaasFunctionBinding.AccessModifier;
import org.hpcclab.msc.object.entity.object.OaasObject;
import org.hpcclab.msc.object.entity.state.OaasObjectState;
import org.hpcclab.msc.object.model.FunctionCallRequest;
import org.hpcclab.msc.object.model.OaasFunctionBindingDto;
import org.hpcclab.msc.object.model.OaasObjectDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import java.util.List;

import static io.restassured.RestAssured.given;
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
    var newFb = obj.getFunctions()
      .stream()
      .filter(f -> f.getFunction().equals("builtin.hls.ts.transcode"))
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
      new OaasFunctionBindingDto(AccessModifier.PUBLIC,"builtin.hls.ts.transcode")
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
  }

}
