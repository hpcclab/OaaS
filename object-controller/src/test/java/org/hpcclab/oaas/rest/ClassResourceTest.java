package org.hpcclab.oaas.rest;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import org.hamcrest.Matchers;
import org.hpcclab.oaas.ArangoResource;
import org.hpcclab.oaas.TestUtils;
import org.hpcclab.oaas.controller.rest.ModuleService;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.object.OaasObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import javax.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@QuarkusTestResource(ArangoResource.class)
class ClassResourceTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClassResourceTest.class);
  @Test
  void create() {
    TestUtils.createBatchYaml(TestUtils.DUMMY_BATCH);
    given()
      .when().get("/api/classes")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("items.name", hasItems("test.dummy.simple", "test.dummy.compound"));
    given()
      .when().get("/api/classes/test.dummy.simple")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .log().all();
  }

  @Test
  void inheritance() {
    // language=yaml
    var clsText = """
      functions:
        - name: f1
          type: TASK
          outputCls: base
        - name: f2
          type: TASK
          outputCls: base
        - name: f3
          type: TASK
          outputCls: base
      classes:
        - name: base
          stateType: FILES
          objectType: SIMPLE
          stateSpec:
            keySpecs:
              - name: k1
          functions:
            - function: f1
              name: func1
        - name: test.add-func
          parents: [base]
          functions:
            - function: f2
              name: func2
        - name: test.add-key
          parents: [base]
          stateSpec:
            keySpecs:
              - name: k2
        - name: test.override-func
          parents: [base, test.add-func, test.add-key]
          functions:
            - function: f3
              name: func1
      """;
    TestUtils.createBatchYaml(clsText);
    given()
      .when().get("/api/classes/base")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("resolved.functions.func1.name", is("func1"))
      .body("resolved.functions.func1.function", is("f1"))
      .body("resolved.keySpecs.k1.name", is("k1"))
      .log().ifValidationFails();

    given()
      .when().get("/api/classes/test.add-func")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("resolved.functions.func1.name", is("func1"))
      .body("resolved.functions.func1.function", is("f1"))
      .body("resolved.functions.func2.name", is("func2"))
      .body("resolved.functions.func2.function", is("f2"))
      .body("resolved.keySpecs.k1.name", is("k1"))
      .log().ifValidationFails();

    given()
      .when().get("/api/classes/test.add-key")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("resolved.functions.func1.name", is("func1"))
      .body("resolved.functions.func1.function", is("f1"))
      .body("resolved.keySpecs.k1.name", is("k1"))
      .body("resolved.keySpecs.k2.name", is("k2"))
      .log().ifValidationFails();

    given()
      .when().get("/api/classes/test.override-func")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("resolved.functions.func1.name", is("func1"))
      .body("resolved.functions.func1.function", is("f3"))
      .body("resolved.functions.func2.name", is("func2"))
      .body("resolved.functions.func2.function", is("f2"))
      .body("resolved.keySpecs.k1.name", is("k1"))
      .body("resolved.keySpecs.k2.name", is("k2"))
      .log().ifValidationFails();
  }


  @Test
  void testCyclic() {
    // language=yaml
    var clsText = """
      functions: []
      classes:
        - name: c1
          stateType: FILES
          objectType: SIMPLE
          parents: [c3]
        - name: c2
          stateType: FILES
          objectType: SIMPLE
          parents: [c1]
        - name: c3
          stateType: FILES
          objectType: SIMPLE
          parents: [c2]
        """;
    given()
      .contentType("text/x-yaml")
      .body(clsText)
      .when().post("/api/modules?update=true")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(400);
  }


}
