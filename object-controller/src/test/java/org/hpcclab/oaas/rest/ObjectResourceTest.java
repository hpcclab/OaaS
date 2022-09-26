package org.hpcclab.oaas.rest;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.hpcclab.oaas.ArangoResource;
import org.hpcclab.oaas.TestUtils;
import org.hpcclab.oaas.invocation.function.FunctionRouter;
import org.hpcclab.oaas.model.oal.ObjectAccessLangauge;
import org.hpcclab.oaas.model.object.ObjectConstructRequest;
import org.hpcclab.oaas.model.object.ObjectReference;
import org.hpcclab.oaas.model.object.OaasObject;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(ArangoResource.class)
public class ObjectResourceTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(ObjectResourceTest.class);

  @Inject
  FunctionRouter router;


  @Test
  void testCreate() {
    var request = new ObjectConstructRequest();
    request.setCls("builtin.basic.file");
    var obj = TestUtils.create(request);
    TestUtils.getObject(obj.getId());
    assertTrue(TestUtils.listObject().size() >= 1);
    TestUtils.getObject(obj.getId());
  }

  @Test
  void testCompound() {
    TestUtils.createBatchYaml(TestUtils.DUMMY_BATCH);
    var oReq1 = new ObjectConstructRequest();
    oReq1.setCls("test.dummy.simple");
    var obj1 = TestUtils.create(oReq1);
    var oReq2 = new ObjectConstructRequest();
    oReq2.setCls("test.dummy.simple");
    var obj2 = TestUtils.create(oReq2);
    var compoundReq = new ObjectConstructRequest();
    compoundReq.setCls("test.dummy.compound");
    compoundReq.setRefs(Set.of(
        new ObjectReference().setName("obj1").setObjId(obj1.getId()),
        new ObjectReference().setName("obj2").setObjId(obj2.getId())
      )
    );
    TestUtils.create(compoundReq);
  }
}
