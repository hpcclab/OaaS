package org.hpcclab.oaas;

import io.restassured.common.mapper.TypeRef;
import io.vertx.core.json.Json;
import org.hamcrest.Matchers;
import org.hpcclab.oaas.model.*;
import org.hpcclab.oaas.model.cls.OaasClassDto;
import org.hpcclab.oaas.model.function.FunctionCallRequest;
import org.hpcclab.oaas.model.function.OaasFunctionBindingDto;
import org.hpcclab.oaas.model.function.OaasFunctionDto;
import org.hpcclab.oaas.model.object.DeepOaasObjectDto;
import org.hpcclab.oaas.model.object.OaasObjectDto;

import javax.ws.rs.core.MediaType;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;


public class TestUtils {

  public static final String DUMMY_FUNCTION = """
    - name: test.dummy.resource
      type: TASK
      outputCls: builtin.basic.file
      validation: {}
      provision:
        job: {}
    - name: test.dummy.compound
      type: MACRO
      outputCls: builtin.basic.compound
      validation: {}
      macro:
          steps:
            - funcName: builtin.logical.copy
              target: obj1
              as: new_obj1
              inputRefs: []
            - funcName: builtin.logical.copy
              target: obj2
              as: new_obj2
              inputRefs: []
          exports:
            - from: new_obj1
              as: obj1
            - from: new_obj2
              as: obj2
    """;

  public static List<OaasObjectDto> listObject() {
    return Arrays.asList(given()
      .when().get("/api/objects")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .log().ifValidationFails()
      .extract().body().as(OaasObjectDto[].class));
  }

  public static OaasObjectDto create(OaasObjectDto o) {
    return given()
      .contentType(MediaType.APPLICATION_JSON)
      .body(Json.encodePrettily(o))
      .when().post("/api/objects")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("id", Matchers.notNullValue())
      .extract().body().as(OaasObjectDto.class);
  }

  public static OaasObjectDto getObject(UUID id) {
    return given()
      .pathParam("id", id.toString())
      .when().get("/api/objects/{id}")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("id", Matchers.equalTo(id.toString()))
      .extract().body().as(OaasObjectDto.class);
  }

  public static OaasClassDto getClass(String name) {
    return given()
      .pathParam("name", name)
      .when().get("/api/classes/{name}")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("name", Matchers.equalTo(name))
      .extract().body().as(OaasClassDto.class);
  }


  public static DeepOaasObjectDto getObjectDeep(UUID id) {
    return given()
      .pathParam("id", id.toString())
      .when().get("/api/objects/{id}/deep")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("id", Matchers.equalTo(id.toString()))
      .extract().body().as(DeepOaasObjectDto.class);
  }

  public static TaskContext getTaskContext(UUID id) {
    return given()
      .pathParam("id", id.toString())
      .when().get("/api/objects/{id}/context")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("output.id", Matchers.equalTo(id.toString()))
      .extract().body().as(TaskContext.class);
  }

  public static OaasObjectDto bind(OaasObjectDto obj, List<OaasFunctionBindingDto> fd) {
    return given()
      .contentType(MediaType.APPLICATION_JSON)
      .body(Json.encodePrettily(fd))
      .pathParam("oid", obj.getId().toString())
      .when().post("/api/objects/{oid}/binds")
      .then()
      .log().ifValidationFails()
//      .log().all()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("id", Matchers.notNullValue())
      .extract().body().as(OaasObjectDto.class);
  }

  public static OaasObjectDto reactiveCall(FunctionCallRequest request) {
    return given()
      .contentType(MediaType.APPLICATION_JSON)
      .body(Json.encodePrettily(request))
      .pathParam("oid", request.getTarget().toString())
      .when().post("/api/objects/{oid}/r-exec")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("id", Matchers.notNullValue())
      .extract().body().as(OaasObjectDto.class);
  }

  public static List<OaasFunctionDto> createFunctionYaml(String function) {
    return given()
      .contentType("text/x-yaml")
      .body(function)
      .when().post("/api/functions?update=true")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .extract().body().as(new TypeRef<List<OaasFunctionDto>>(){});
  }
}
