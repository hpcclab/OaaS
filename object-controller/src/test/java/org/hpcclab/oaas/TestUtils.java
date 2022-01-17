package org.hpcclab.oaas;

import io.restassured.common.mapper.TypeRef;
import io.vertx.core.json.Json;
import org.hamcrest.Matchers;
import org.hpcclab.oaas.iface.service.BatchService;
import org.hpcclab.oaas.model.*;
import org.hpcclab.oaas.model.function.ObjectAccessExpression;
import org.hpcclab.oaas.model.proto.OaasClass;
import org.hpcclab.oaas.model.proto.OaasFunction;
import org.hpcclab.oaas.model.object.DeepOaasObject;
import org.hpcclab.oaas.model.proto.OaasObject;

import javax.ws.rs.core.MediaType;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;


public class TestUtils {

  // language=yaml
  public static final String DUMMY_BATCH = """
    functions:
      - name: test.dummy.task
        type: TASK
        outputCls: test.dummy.simple
        validation: {}
        provision:
          job: {}
      - name: test.dummy.macro
        type: MACRO
        outputCls: test.dummy.compound
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
    classes:
      - name: test.dummy.simple
        stateType: FILE
        objectType: SIMPLE
        functions:
        - access: PUBLIC
          function: builtin.logical.copy
        - access: PUBLIC
          function: test.dummy.task
      - name: test.dummy.compound
        objectType: COMPOUND
        functions:
          - access: PUBLIC
            function: builtin.logical.copy
          - access: PUBLIC
            function: test.dummy.macro
    """;


  public static List<OaasObject> listObject() {
    return Arrays.asList(given()
      .when().get("/api/objects")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .log().ifValidationFails()
      .extract().body().as(OaasObject[].class));
  }

  public static OaasObject create(OaasObject o) {
    return given()
      .contentType(MediaType.APPLICATION_JSON)
      .body(Json.encodePrettily(o))
      .when().post("/api/objects")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("id", Matchers.notNullValue())
      .extract().body().as(OaasObject.class);
  }

  public static OaasObject getObject(UUID id) {
    return given()
      .pathParam("id", id.toString())
      .when().get("/api/objects/{id}")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("id", Matchers.equalTo(id.toString()))
      .extract().body().as(OaasObject.class);
  }

  public static OaasClass getClass(String name) {
    return given()
      .pathParam("name", name)
      .when().get("/api/classes/{name}")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("name", Matchers.equalTo(name))
      .extract().body().as(OaasClass.class);
  }


  public static DeepOaasObject getObjectDeep(UUID id) {
    return given()
      .pathParam("id", id.toString())
      .when().get("/api/objects/{id}/deep")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("id", Matchers.equalTo(id.toString()))
      .extract().body().as(DeepOaasObject.class);
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

  public static OaasObject reactiveCall(ObjectAccessExpression request) {
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
      .extract().body().as(OaasObject.class);
  }

  public static BatchService.Batch createBatchYaml(String clsText) {
    return given()
      .contentType("text/x-yaml")
      .body(clsText)
      .when().post("/api/batch?update=true")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .extract().body().as(BatchService.Batch.class);
  }

  public static List<OaasFunction> createFunctionYaml(String function) {
    return given()
      .contentType("text/x-yaml")
      .body(function)
      .when().post("/api/functions?update=true")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .extract().body().as(new TypeRef<List<OaasFunction>>(){});
  }
}
