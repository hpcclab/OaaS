package org.hpcclab.msc.resourcetest;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.hamcrest.Matchers;
import org.hpcclab.msc.object.model.RootMscObjectCreating;
import org.hpcclab.msc.object.resource.ObjectResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;

@QuarkusTest
@TestHTTPEndpoint(ObjectResource.class)
public class ObjectResourceTest {
  @BeforeAll
  static void setup() {
    RestAssured.filters(new RequestLoggingFilter(),
      new ResponseLoggingFilter());
  }

  @Test
  void test() {
    var root = new RootMscObjectCreating()
      .setType("test")
      .setSourceUrl("http://test/test.m3u8");
    var id = given()
      .contentType(MediaType.APPLICATION_JSON)
      .body(root)
      .when().post()
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("id", Matchers.notNullValue())
      .extract().body().<String>path("id");


    given()
      .when().get()
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("size()",Matchers.greaterThanOrEqualTo(1));


    given()
      .pathParam("id", id)
      .when().get("{id}")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("id", Matchers.equalTo(id));


    given()
      .when().get("test")
      .then()
      .statusCode(404);
  }
}
