package org.hpcclab.oaas.repository.function;


import org.hpcclab.oaas.model.proto.OaasClass;
import org.hpcclab.oaas.model.proto.OaasFunction;
import org.hpcclab.oaas.model.proto.OaasObject;
import org.hpcclab.oaas.repository.OaasObjectRepository;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

class FunctionRouterTest {

  FunctionRouter router;
  OaasObjectRepository objectRepo;

  @BeforeEach
  public void setup() {
    List<OaasObject> objects = List.of();
    List<OaasClass> classes = List.of();
    List<OaasFunction> functions = List.of();
    var cl = TestUtil.mockContextLoader(objects, classes, functions);
    objectRepo = cl.objectRepo;
    var logical = new LogicalFunctionHandler();
    logical.objectRepo = objectRepo;
    var task = new TaskFunctionHandler();
    task.objectRepo = objectRepo;
    var macro = new MacroFunctionHandler();
    macro.contextLoader = cl;
    macro.objectRepo = objectRepo;
    router = new FunctionRouter(logical, macro, task, cl);
    macro.router = router;
  }

}
