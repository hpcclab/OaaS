package org.hpcclab.oaas.test;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.cls.OClassConfig;
import org.hpcclab.oaas.model.function.*;
import org.hpcclab.oaas.model.function.Dataflows.DataMapping;
import org.hpcclab.oaas.model.function.Dataflows.Transformation;
import org.hpcclab.oaas.model.object.OObjectType;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.model.qos.QosConstraint;
import org.hpcclab.oaas.model.state.KeyAccessModifier;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.hpcclab.oaas.model.state.StateSpecification;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.ClassResolver;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.hpcclab.oaas.repository.MapEntityRepository;

import java.util.List;

public class MockupData {
  public static final String CLS_1_KEY = "ex.cls1";

  public static final OFunction FUNC_NEW = new OFunction()
    .setName("new")
    .setPkg("builtin")
    .setType(FunctionType.BUILTIN);
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
    .setType(FunctionType.TASK)
    .setImmutable(true)
    .setStatus(new OFunctionDeploymentStatus()
      .setCondition(DeploymentCondition.RUNNING)
      .setInvocationUrl("http://localhost:8080")
    );

  public static final OFunction MACRO_FUNC_1 = new OFunction()
    .setName("macro1")
    .setPkg("ex")
    .setType(FunctionType.MACRO)
    .setMacro(Dataflows.Spec.builder()
      .steps(List.of(
        Dataflows.Step.builder()
          .function("f1")
          .target("@")
          .argRefs(DSMap.of("key1", "arg1"))
          .as("tmp1")
          .args(DSMap.of("STEP", "1"))
          .build(),
        Dataflows.Step.builder()
          .function("f3")
          .target("tmp1")
          .as("tmp2")
          .args(DSMap.of("STEP", "2"))
          .build(),
        Dataflows.Step.builder()
          .function("f3")
          .target("tmp2")
          .as("tmp3")
          .args(DSMap.of("STEP", "3"))
          .build()
      ))
      .output("tmp3")
      .respBody(List.of(
        DataMapping.builder()
          .fromBody("tmp1")
          .transforms(
            List.of(
              new Transformation("$.n", "step1")
            )
          ).build(),
        DataMapping.builder()
          .fromBody("tmp2")
          .transforms(
            List.of(
              new Transformation("$.n", "step2")
            )
          ).build(),
        DataMapping.builder()
          .fromBody("tmp3")
          .transforms(
            List.of(
              new Transformation("$.n", "step3")
            )
          ).build()
      ))
      .build()
    );

  public static final OFunction MACRO_FUNC_2 = new OFunction()
    .setName("macro2")
    .setPkg("ex")
    .setType(FunctionType.MACRO)
    .setMacro(Dataflows.Spec.builder()
      .steps(List.of(
        Dataflows.Step.builder()
          .function("f3")
          .target("@")
          .argRefs(DSMap.of("ADD", "ADD1"))
          .as("tmp1")
          .args(DSMap.of("STEP", "1.1"))
          .build(),
        Dataflows.Step.builder()
          .function("f3")
          .target("@")
          .argRefs(DSMap.of("ADD", "ADD2"))
          .as("tmp2")
          .args(DSMap.of("STEP", "1.2"))
          .build(),
        Dataflows.Step.builder()
          .function("f3")
          .target("@")
          .argRefs(DSMap.of("ADD", "ADD3"))
          .as("tmp3")
          .args(DSMap.of("STEP", "1.3"))
          .build(),
        Dataflows.Step.builder()
          .function("f3")
          .target("tmp1")
          .as("tmp4")
          .mappings(List.of(
            DataMapping.builder().fromObj("tmp2").transforms(List.of(new Transformation("$.n", "tmp2"))).build(),
            DataMapping.builder().fromObj("tmp3").transforms(List.of(new Transformation("$.n", "tmp3"))).build()
          ))
          .args(DSMap.of("STEP", "2", "ADD", "0"))
          .build()
      ))
      .output("tmp4")
      .build()
    );

  public static final OFunction CHAIN_FUNC_1 = MACRO_FUNC_1
    .copy()
    .setName("chain1")
    .setType(FunctionType.CHAIN);
  public static final OClass CLS_1 = new OClass()
    .setName("cls1")
    .setPkg("ex")
    .setObjectType(OObjectType.SIMPLE)
    .setConfig(new OClassConfig())
    .setConstraint(QosConstraint.builder().build())
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
        .setFunction("builtin.new")
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
        .setName(MACRO_FUNC_2.getName())
        .setFunction(MACRO_FUNC_2.getKey())
        .setOutputCls(CLS_1_KEY),
      new FunctionBinding()
        .setName(CHAIN_FUNC_1.getName())
        .setFunction(CHAIN_FUNC_1.getKey())
        .setOutputCls(CLS_1_KEY)
    ));
  public static final OClass CLS_2 = new OClass()
    .setName("cls2")
    .setPkg("ex")
    .setConfig(new OClassConfig())
    .setObjectType(OObjectType.SIMPLE)
    .setParents(List.of(CLS_1.getKey()))
    .setConstraint(QosConstraint.builder().build())
    ;

  static FunctionRepository fnRepo;
  static ClassRepository clsRepo;

  private MockupData() {
  }

  public static MutableMap<String, OClass> testClasses() {
    var clsResolver = new ClassResolver(null);
    CLS_1.validate();
    CLS_2.validate();
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
        MACRO_FUNC_2.copy(),
        CHAIN_FUNC_1.copy()
      )
      .groupByUniqueKey(OFunction::getKey);
  }


  public static FunctionRepository fnRepo() {
    if (fnRepo==null)
      fnRepo = new MapEntityRepository.MapFnRepository(MockupData.testFunctions());
    return fnRepo;
  }

  public static ClassRepository clsRepo() {
    if (clsRepo==null)
      clsRepo = new MapEntityRepository.MapClsRepository(MockupData.testClasses());
    return clsRepo;
  }
}
