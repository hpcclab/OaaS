package org.hpcclab.oaas.repository.function;

import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.function.OaasFunctionBinding;
import org.hpcclab.oaas.model.function.OaasFunctionType;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.object.ObjectOrigin;
import org.hpcclab.oaas.model.object.ObjectType;

import java.util.List;
import java.util.Set;

public class MockupData {

  public static final OaasClass CLS_1 = new OaasClass()
    .setName("cls1")
    .setObjectType(ObjectType.SIMPLE)
    .setFunctions(Set.of(
      new OaasFunctionBinding()
        .setName("func1")
        .setFunction("func1")
    ));

  public static List<OaasClass> testClasses () {
    return List.of(
      CLS_1
    );
  }
  public static List<OaasFunction> testFunctions () {
    return List.of(
      new OaasFunction()
        .setName("func1")
        .setType(OaasFunctionType.TASK)
        .setOutputCls("cls1")
    );
  }

  public static List<OaasObject> testObjects() {
    var o1 = OaasObject.createFromClasses(CLS_1);
    o1.setId("o1");
    o1.setOrigin(new ObjectOrigin());
    return List.of(o1);
  }
}
