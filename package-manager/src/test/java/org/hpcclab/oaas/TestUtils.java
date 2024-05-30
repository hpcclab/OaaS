package org.hpcclab.oaas;

import io.restassured.common.mapper.TypeRef;
import org.hamcrest.Matchers;
import org.hpcclab.oaas.model.pkg.OPackage;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.cls.OClass;

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
              - function: copy
                target: new_obj1
                as: new_obj2
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
    """;


  public static List<OClass> listClasses() {
    return given()
      .when().get("/api/classes")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .log().ifValidationFails()
      .extract().body().as( new TypeRef<Pagination<OClass>>() {})
      .getItems();
  }

  public static OClass getClass(String name) {
    return given()
      .pathParam("name", name)
      .when().get("/api/classes/{name}")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("name", Matchers.equalTo(name))
      .extract().body().as(OClass.class);
  }

  public static void createBatchYaml(String clsText) {
    given()
      .contentType("text/x-yaml")
      .body(clsText)
      .queryParam("update", "true")
      .when().post("/api/packages")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .extract().body().as(OPackage.class);
  }
}
