package org.hpcclab.oaas.invocation;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.function.*;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.object.ObjectOrigin;
import org.hpcclab.oaas.model.object.ObjectStatus;
import org.hpcclab.oaas.model.object.ObjectType;
import org.hpcclab.oaas.repository.ClassResolver;

import java.util.List;
import java.util.Map;

public class MockupData {


  public static final OaasFunction FUNC_1 = new OaasFunction()
    .setName("func1")
    .setPkg("ex")
    .setType(FunctionType.TASK)
    .setOutputCls("cls1");


  public static final OaasFunction MACRO_FUNC_1 = new OaasFunction()
    .setName("macroFunc2")
    .setPkg("ex")
    .setType(FunctionType.MACRO)
    .setOutputCls("cls1")
    .setMacro(new Dataflow()
      .setSteps(List.of(
        new DataflowStep()
          .setFunction(FUNC_1.getName())
          .setTarget("$")
          .setArgRefs(Map.of("key1","arg1"))
          .setAs("tmp1"),
        new DataflowStep()
          .setFunction(FUNC_1.getName())
          .setTarget("tmp1")
          .setAs("tmp2")
      ))
      .setExport("tmp2")
    );

  public static final OaasClass CLS_1 = new OaasClass()
    .setName("cls1")
    .setPkg("ex")
    .setObjectType(ObjectType.SIMPLE)
    .setFunctions(List.of(
      new FunctionBinding()
        .setName(FUNC_1.getName())
        .setFunction(FUNC_1.getName()),
      new FunctionBinding()
        .setName(MACRO_FUNC_1.getName())
        .setFunction(MACRO_FUNC_1.getName())
    ));

  public static MutableMap<String,OaasClass> testClasses() {
    var clsResolver = new ClassResolver();
    return Lists.fixedSize.of(
        clsResolver.resolve(CLS_1, List.of())
    )
      .groupByUniqueKey(OaasClass::getName);
  }

  public static MutableMap<String,OaasFunction> testFunctions() {
    return Lists.fixedSize.of(
      FUNC_1,
      MACRO_FUNC_1
    )
      .groupByUniqueKey(OaasFunction::getName);
  }

  public static List<OaasObject> testObjects() {
    var l = Lists.mutable.<OaasObject>empty();
    var o1 = OaasObject.createFromClasses(CLS_1);
    o1.setId("o1");
    o1.setOrigin(new ObjectOrigin());
    o1.setStatus(new ObjectStatus());
    l.add(o1);

    var o2 = OaasObject.createFromClasses(CLS_1);
    o2.setId("o2");
    o2.setOrigin(new ObjectOrigin()
      .setParentId(o1.getId())
      .setFuncName(FUNC_1.getName())
    );
    o2.setStatus(new ObjectStatus());
    l.add(o2);

    return l;
  }
}
