package org.hpcclab.msc.senario;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.hamcrest.Matchers;
import org.hpcclab.msc.TestUtils;
import org.hpcclab.msc.object.entity.MscFuncMetadata;
import org.hpcclab.msc.object.entity.MscFunction;
import org.hpcclab.msc.object.entity.object.FileState;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.model.RootMscObjectCreating;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class FunctionCallTest {
  @BeforeAll
  static void setup() {
    RestAssured.filters(new RequestLoggingFilter(),
      new ResponseLoggingFilter());
  }

  @Test
  void testCopy() {
    var root = new MscObject()
      .setType(MscObject.Type.RESOURCE)
      .setState(new FileState().setFileUrl("http://test/test.m3u8"));
    root = TestUtils.create(root);

    var newObj = given()
      .contentType(MediaType.APPLICATION_JSON)
      .body(Map.of())
      .pathParam("oid", root.getId().toString())
      .pathParam("funcName", "buildin.logical.copy")
      .when().post("/api/objects/{oid}/rf-call/{funcName}")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("id", Matchers.notNullValue())
      .extract().body().as(MscObject.class);
  }
}
