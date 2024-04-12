package org.hpcclab.oaas.test;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.cls.OClassConfig;
import org.hpcclab.oaas.model.function.*;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.model.object.OObjectType;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.model.state.KeyAccessModifier;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.model.state.StateSpecification;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.ClassResolver;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.hpcclab.oaas.repository.ObjectRepoManager;

import java.util.List;

public class MockupData {

  public static final OFunction FUNC_NEW = new OFunction()
    .setName("new")
    .setPkg("builtin.logical")
    .setType(FunctionType.LOGICAL);
  public static final OFunction FUNC_1 = new OFunction()
    .setName("func1")
    .setPkg("ex")
    .setType(FunctionType.TASK)
    .setStatus(new OFunctionDeploymentStatus()
      .setCondition(DeploymentCondition.RUNNING)
      .setInvocationUrl("http://localhost:8080")
    );
  public static final OFunction FUNC_2 = new OFunction()
    .setName("im-fn")
    .setPkg("ex")
    .setType(FunctionType.IM_TASK)
    .setStatus(new OFunctionDeploymentStatus()
      .setCondition(DeploymentCondition.RUNNING)
      .setInvocationUrl("http://localhost:8080")
    );
  public static final OFunction MACRO_FUNC_1 = new OFunction()
    .setName("macroFunc1")
    .setPkg("ex")
    .setType(FunctionType.MACRO)
    .setMacro(new MacroSpec()
      .setSteps(List.of(
        new DataflowStep()
          .setFunction("f1")
          .setTarget("$")
          .setArgRefs(DSMap.of("key1", "arg1"))
          .setAs("tmp1")
          .setArgs(DSMap.of("STEP", "1")),
        new DataflowStep()
          .setFunction("f3")
          .setTarget("tmp1")
          .setAs("tmp2")
          .setArgs(DSMap.of("STEP", "2")),
        new DataflowStep()
          .setFunction("f3")
          .setTarget("tmp2")
          .setAs("tmp3")
          .setArgs(DSMap.of("STEP", "3"))
      ))
      .setExport("tmp3")
    );
  public static final OFunction ATOMIC_MACRO_FUNC = new OFunction()
    .setName("atomic-macro")
    .setPkg("ex")
    .setType(FunctionType.MACRO)
    .setMacro(new MacroSpec()
      .setSteps(List.of(
        new DataflowStep()
          .setFunction("f3")
          .setTarget("$")
          .setAs("tmp1")
          .setArgs(DSMap.of("STEP", "1.1")),
        new DataflowStep()
          .setFunction("f3")
          .setTarget("$")
          .setAs("tmp2")
          .setArgs(DSMap.of("STEP", "1.2")),
        new DataflowStep()
          .setFunction("f3")
          .setTarget("tmp1")
          .setInputRefs(List.of("tmp2"))
          .setAs("tmp3")
          .setArgs(DSMap.of("STEP", "2"))
      ))
      .setExport("tmp3")
    );
  public static final String CLS_1_KEY = "ex.cls1";
  public static final OClass CLS_1 = new OClass()
    .setName("cls1")
    .setPkg("ex")
    .setObjectType(OObjectType.SIMPLE)
    .setConfig(new OClassConfig())
    .setStateSpec(new StateSpecification()
      .setKeySpecs(
        List.of(
          new KeySpecification()
            .setName("k1")
            .setAccess(KeyAccessModifier.PUBLIC)
        )
      ))
    .setFunctions(List.of(
      new FunctionBinding()
        .setName("new")
        .setFunction("builtin.logical.new")
        .setOutputCls(CLS_1_KEY)
        .setNoMain(true)
      ,
      new FunctionBinding()
        .setName("f1")
        .setFunction(FUNC_1.getKey())
        .setOutputCls(CLS_1_KEY)
        .setDefaultArgs(DSMap.of("aa", "aa", "aaa", "aaa")),
      new FunctionBinding()
        .setName("func2")
        .setFunction(FUNC_1.getKey())
        .setOutputCls(null),
      new FunctionBinding()
        .setName("f3")
        .setFunction(FUNC_1.getKey())
        .setImmutable(true)
        .setInputTypes(List.of(CLS_1_KEY))
        .setOutputCls(CLS_1_KEY),
      new FunctionBinding()
        .setName(FUNC_2.getName())
        .setFunction(FUNC_2.getKey())
        .setOutputCls(CLS_1_KEY),
      new FunctionBinding()
        .setName(MACRO_FUNC_1.getName())
        .setFunction(MACRO_FUNC_1.getKey())
        .setOutputCls(CLS_1_KEY),
      new FunctionBinding()
        .setName(ATOMIC_MACRO_FUNC.getName())
        .setFunction(ATOMIC_MACRO_FUNC.getKey())
        .setOutputCls(CLS_1_KEY)
    ));
  public static final OClass CLS_2 = new OClass()
    .setName("cls2")
    .setPkg("ex")
    .setConfig(new OClassConfig())
    .setObjectType(OObjectType.SIMPLE)
    .setParents(List.of(CLS_1.getKey()));
  public static final OObject OBJ_1 = OObject.createFromClasses(CLS_1)
    .setId("o1")
    .setState(new OaasObjectState()
      .setVerIds(DSMap.of("k1", "kkkk"))
    );
  public static final OObject OBJ_2 = OObject.createFromClasses(CLS_1)
    .setId("o2");

  private MockupData() {
  }

  public static MutableMap<String, OClass> testClasses() {
    var clsResolver = new ClassResolver(null);
    var cls1 = clsResolver.resolve(CLS_1.copy(), List.of());
    var cls2 = clsResolver.resolve(CLS_2.copy(), List.of(cls1));
    return Lists.fixedSize.of(
        cls1,
        cls2
      )
      .groupByUniqueKey(OClass::getKey);
  }

  public static MutableMap<String, OFunction> testFunctions() {
    return Lists.fixedSize.of(
        FUNC_NEW.copy(),
        FUNC_1.copy(),
        FUNC_2.copy(),
        MACRO_FUNC_1.copy(),
        ATOMIC_MACRO_FUNC.copy()
      )
      .groupByUniqueKey(OFunction::getKey);
  }

  public static List<OObject> testObjects() {
    var l = Lists.mutable.<OObject>empty();
    l.add(OBJ_1.copy());
    l.add(OBJ_2.copy());
    return l;
  }


  public static void persistMock(ObjectRepoManager objectRepoManager,
                                 ClassRepository clsRepo,
                                 FunctionRepository fnRepo) {
    for (OClass cls : testClasses()) {
      cls.validate();
      clsRepo.persist(cls);
    }
    for (OFunction func : testFunctions()) {
      func.validate();
      fnRepo.persist(func);
    }
    for (OObject testObject : testObjects()) {
      objectRepoManager.persistAsync(testObject)
        .await().indefinitely();
    }
  }

}
