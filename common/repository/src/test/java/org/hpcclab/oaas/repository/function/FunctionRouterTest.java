package org.hpcclab.oaas.repository.function;


import io.vertx.core.json.Json;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.oal.ObjectAccessLangauge;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.DefaultIdGenerator;
import org.hpcclab.oaas.repository.OaasObjectFactory;
import org.hpcclab.oaas.repository.OaasObjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class FunctionRouterTest {

  FunctionRouter router;
  OaasObjectRepository objectRepo;

  @BeforeEach
  public void setup() {
    List<OaasObject> objects = MockupData.testObjects();
    List<OaasClass> classes = MockupData.testClasses();
    List<OaasFunction> functions = MockupData.testFunctions();
    var cl = TestUtil.mockContextLoader(objects, classes, functions);
    objectRepo = cl.objectRepo;
    var idGen = new DefaultIdGenerator();
    var objectFactory = new OaasObjectFactory(idGen);
    var logical = new LogicalFunctionHandler(idGen);
    var task = new TaskFunctionHandler();
    task.objectFactory = objectFactory;
    var macro = new MacroFunctionHandler();
    macro.contextLoader = cl;
    macro.objectFactory = objectFactory;
    router = new FunctionRouter(logical, macro, task, cl);
    macro.router = router;
  }

  @Test
  void testTaskFunction() {
    var oal = ObjectAccessLangauge.parse("o1:func1()");
    var ctx = router.invoke(oal)
      .await().indefinitely();
    System.out.println(Json.encodePrettily(ctx));
  }
}
