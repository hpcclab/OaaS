package org.hpcclab.oaas.rest;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.hamcrest.Matchers;
import org.hpcclab.oaas.ArangoResource;
import org.hpcclab.oaas.TestUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@QuarkusTestResource(ArangoResource.class)
class ClassResourceTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClassResourceTest.class);


  // language=yaml
  String clsText1 = """
      functions:
        - name: f1
          type: TASK
        - name: f2
          type: TASK
        - name: f3
          type: TASK
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
              outputCls: base
        - name: test.add-func
          parents: [base]
          functions:
            - function: f2
              name: func2
              outputCls: base
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
              outputCls: base
      """;

  // language=yaml
  String clsText2 = """
      classes:
        - name: test.add-func
          parents: [base]
          stateSpec:
            keySpecs:
              - name: k3
          functions:
            - function: f2
              name: func2
              outputCls: base
            - function: f3
              name: func3
              outputCls: base
      """;

  @Test
  void create() {
    TestUtils.createBatchYaml(TestUtils.DUMMY_PACKAGE);
    given()
      .when().get("/api/classes")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("items.name", hasItems("simple", "compound"));
    given()
      .when().get("/api/classes/test.dummy.simple")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("functions[1].outputCls", Matchers.nullValue());
  }

  @Test
  void inheritance() {
    TestUtils.createBatchYaml(clsText1);
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
  void testUpdateChild() {
    TestUtils.createBatchYaml(clsText1);
    TestUtils.createBatchYaml(clsText2);
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
      .body("resolved.functions.func3.name", is("func3"))
      .body("resolved.functions.func3.function", is("f3"))
      .body("resolved.keySpecs.k1.name", is("k1"))
      .body("resolved.keySpecs.k3.name", is("k3"))
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
      .body("resolved.functions.func3.name", is("func3"))
      .body("resolved.functions.func3.function", is("f3"))
      .body("resolved.keySpecs.k1.name", is("k1"))
      .body("resolved.keySpecs.k2.name", is("k2"))
      .body("resolved.keySpecs.k3.name", is("k3"))
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
      .when().post("/api/packages?update=true")
      .then()
      .log().ifValidationFails()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(400);
  }


}
