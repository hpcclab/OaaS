package org.hpcclab.msc;

import io.vertx.core.json.Json;
import org.bson.types.ObjectId;
import org.hamcrest.Matchers;
import org.hpcclab.msc.object.entity.MscFuncMetadata;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.model.FunctionCallRequest;

import javax.ws.rs.core.MediaType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;


public class TestUtils {

  public static List<MscObject> listObject() {
    return Arrays.asList(given()
      .when().get("/api/objects")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .extract().body().as(MscObject[].class));
  }

  public static MscObject create(MscObject o) {
    return given()
      .contentType(MediaType.APPLICATION_JSON)
      .body(Json.encodePrettily(o))
      .when().post("/api/objects")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("id", Matchers.notNullValue())
      .extract().body().as(MscObject.class);
  }

  public static MscObject getObject(ObjectId id) {
    return given()
      .pathParam("id", id.toString())
      .when().get("/api/objects/{id}")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("id", Matchers.equalTo(id.toString()))
      .extract().body().as(MscObject.class);
  }

  public static MscObject bind(MscObject obj, List<String> functionNames) {
//    var meta = functionNames.stream()
//      .map(MscFuncMetadata::new)
//      .collect(Collectors.toList());
    return given()
      .contentType(MediaType.APPLICATION_JSON)
      .body(Json.encodePrettily(functionNames))
      .pathParam("oid", obj.getId().toString())
      .when().post("/api/objects/{oid}/binds")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("id", Matchers.notNullValue())
      .extract().body().as(MscObject.class);
  }

  public static MscObject fnCall(FunctionCallRequest request) {
    return given()
      .contentType(MediaType.APPLICATION_JSON)
      .body(Json.encodePrettily(request))
      .pathParam("oid", request.getTarget().toString())
      .when().post("/api/objects/{oid}/rf-call")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("id", Matchers.notNullValue())
      .extract().body().as(MscObject.class);
  }
}
