package org.hpcclab.oaas.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.hamcrest.Matchers;
import org.hpcclab.oaas.TestUtils;
import org.hpcclab.oaas.model.oal.ObjectAccessLangauge;
import org.hpcclab.oaas.model.object.ObjectReference;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.repository.function.FunctionRouter;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class ObjectResourceTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(ObjectResourceTest.class);

  @Inject
  FunctionRouter router;

  OaasObject call(ObjectAccessLangauge oal) {
    return TestUtils.execOal(oal, router);
  }


  @Test
  void testCreate() {
    var root = new OaasObject();
    root.setCls("builtin.basic.file");
    root = TestUtils.create(root);
    TestUtils.getObject(root.getId());
    assertTrue(TestUtils.listObject().size() >= 1);
    TestUtils.getObject(root.getId());
  }

  @Test
  void testCompound() {
    TestUtils.createBatchYaml(TestUtils.DUMMY_BATCH);
    var obj1 = new OaasObject();
    obj1.setCls("test.dummy.simple");
    obj1.setState(new OaasObjectState());
    obj1 = TestUtils.create(obj1);
    var obj2 = new OaasObject();
    obj2.setCls("test.dummy.simple");
    obj2.setState(new OaasObjectState());
    obj2 = TestUtils.create(obj2);
    var compound = new OaasObject();
    compound.setCls("test.dummy.compound");
    compound.setRefs(Set.of(
        new ObjectReference().setName("obj1").setObjId(obj1.getId()),
        new ObjectReference().setName("obj2").setObjId(obj2.getId())
      )
    );
    compound = TestUtils.create(compound);
    given()
      .pathParam("id", compound.getId())
      .when().get("/api/objects/{id}/deep")
      .then()
      .contentType(MediaType.APPLICATION_JSON)
      .statusCode(200)
      .body("id", Matchers.equalTo(compound.getId()))
      .body("refs.name", hasItems("obj1", "obj2"))
      .body("refs.object", hasItems(obj1.getId(), obj2.getId()));
  }
}
