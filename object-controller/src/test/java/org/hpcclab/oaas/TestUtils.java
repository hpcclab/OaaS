package org.hpcclab.oaas;

import io.restassured.common.mapper.TypeRef;
import io.vertx.core.json.Json;
import org.hamcrest.Matchers;
import org.hpcclab.oaas.model.pkg.OaasPackageContainer;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.object.ObjectConstructRequest;
import org.hpcclab.oaas.model.object.ObjectConstructResponse;

import jakarta.ws.rs.core.MediaType;
import java.util.List;

import static io.restassured.RestAssured.given;


public class TestUtils {

  // language=yaml
  public static final String DUMMY_PACKAGE = """
    name: test.dummy
    functions:
      - name: task
        type: TASK
      - name: macro
        type: MACRO
        macro:
            steps:
              - function: copy
                target: $
                as: new_obj1
                args:
                  k1: text_value
                argRefs:
                  k2: k1
                inputRefs: []
              - function: copy
                target: new_obj1
                as: new_obj2
                inputRefs: []
            exports:
              - from: new_obj1
                as: obj1
              - from: new_obj2
                as: obj2
    classes:
      - name: simple
        stateType: FILES
        objectType: SIMPLE
        stateSpec:
          keySpecs:
            - name: test
              provider: s3
        functions:
        - access: PUBLIC
          function: test.dummy.task
          outputCls: void
      - name: compound
        objectType: COMPOUND
        functions:
          - access: PUBLIC
            function: test.dummy.macro
            outputCls: test.dummy.compound
      - name: stream
        objectType: STREAM
        genericType: test.dummy.simple
        functions: []
    """;


  public static List<OaasObject> listObject() {
    return given()
      .when().get("/api/objects")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .log().ifValidationFails()
      .extract().body().as( new TypeRef<Pagination<OaasObject>>() {})
      .getItems();
  }
  public static List<OaasClass> listClasses() {
    return given()
      .when().get("/api/classes")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .log().ifValidationFails()
      .extract().body().as( new TypeRef<Pagination<OaasClass>>() {})
      .getItems();
  }

  public static OaasObject create(ObjectConstructRequest o) {
    return given()
      .contentType(MediaType.APPLICATION_JSON)
      .body(Json.encodePrettily(o))
      .when().post("/api/object-construct")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("object.id", Matchers.notNullValue())
      .extract().body().as(ObjectConstructResponse.class)
      .getObject();
  }

  public static OaasObject getObject(String id) {
    return given()
      .pathParam("id", id)
      .when().get("/api/objects/{id}")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("id", Matchers.equalTo(id))
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

  public static OaasPackageContainer createBatchYaml(String clsText) {
    return given()
      .contentType("text/x-yaml")
      .body(clsText)
      .when().post("/api/packages?update=true")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .extract().body().as(OaasPackageContainer.class);
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
