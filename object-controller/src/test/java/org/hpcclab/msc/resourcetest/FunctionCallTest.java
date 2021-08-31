package org.hpcclab.msc.resourcetest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.hamcrest.Matchers;
import org.hpcclab.msc.object.entity.MscFunction;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.model.RootMscObjectCreating;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class FunctionCallTest {
  @BeforeAll
  static void setup() {
    RestAssured.filters(new RequestLoggingFilter(),
      new ResponseLoggingFilter());
  }

  @Test
  void test() {
    var root = new RootMscObjectCreating()
      .setSourceUrl("http://test/test.m3u8");
    var obj = given()
      .contentType(MediaType.APPLICATION_JSON)
      .body(root)
      .when().post("/api/objects")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("id", Matchers.notNullValue())
      .extract().body().as(MscObject.class);

    var baseFunc = new MscFunction()
      .setName("buildIn.test")
      .setType("test");

    var func = given()
      .contentType(MediaType.APPLICATION_JSON)
      .body(baseFunc)
      .when().post("/api/functions")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("id", Matchers.notNullValue())
      .extract().body().as(MscFunction.class);

    // bind function to object
    obj = given()
      .contentType(MediaType.APPLICATION_JSON)
      .body(List.of(func.toMeta()))
      .pathParam("oid", obj.getId().toString())
      .when().post("/api/objects/{oid}/binds")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("id", Matchers.notNullValue())
      .extract().body().as(MscObject.class);

    var newObj = given()
      .contentType(MediaType.APPLICATION_JSON)
      .body(Map.of())
      .pathParam("oid", obj.getId().toString())
      .pathParam("funcName", func.getName())
      .when().post("/api/objects/{oid}/lazy-func-call/{funcName}")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("id", Matchers.notNullValue())
      .extract().body().as(MscObject.class);
  }
}
