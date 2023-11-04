package org.hpcclab.oaas.test;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
import org.hpcclab.oaas.invocation.RepoContextLoader;
import org.hpcclab.oaas.model.cls.ClassConfig;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.function.*;
import org.hpcclab.oaas.model.invocation.InvocationNode;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.object.ObjectStatus;
import org.hpcclab.oaas.model.object.ObjectType;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.model.state.KeyAccessModifier;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.model.state.StateSpecification;
import org.hpcclab.oaas.repository.*;

import java.util.List;

public class MockupData {

  private MockupData(){}
  public static final OaasFunction FUNC_NEW = new OaasFunction()
    .setName("new")
    .setPkg("builtin.logical")
    .setType(FunctionType.LOGICAL);
  public static final OaasFunction FUNC_1 = new OaasFunction()
    .setName("func1")
    .setPkg("ex")
    .setType(FunctionType.TASK)
    .setDeploymentStatus(new FunctionDeploymentStatus()
      .setCondition(DeploymentCondition.RUNNING)
      .setInvocationUrl("http://localhost:8080")
    );

  public static final OaasFunction FUNC_2 = new OaasFunction()
    .setName("im-fn")
    .setPkg("ex")
    .setType(FunctionType.IM_TASK)
    .setDeploymentStatus(new FunctionDeploymentStatus()
      .setCondition(DeploymentCondition.RUNNING)
      .setInvocationUrl("http://localhost:8080")
    );

  public static final OaasFunction MACRO_FUNC_1 = new OaasFunction()
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

  public static final OaasFunction ATOMIC_MACRO_FUNC = new OaasFunction()
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
  public static final OaasClass CLS_1 = new OaasClass()
    .setName("cls1")
    .setPkg("ex")
    .setObjectType(ObjectType.SIMPLE)
    .setConfig(new ClassConfig())
    .setStateSpec(new StateSpecification()
      .setKeySpecs(
        List.of(
          new KeySpecification()
            .setName("k1")
            .setAccess(KeyAccessModifier.PUBLIC)
            .setProvider("s3")
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
        .setForceImmutable(true)
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

  public static final OaasClass CLS_2 = new OaasClass()
    .setName("cls2")
    .setPkg("ex")
    .setConfig(new ClassConfig())
    .setObjectType(ObjectType.SIMPLE)
    .setParents(List.of(CLS_1.getKey()));

  public static final OaasObject OBJ_1 = OaasObject.createFromClasses(CLS_1)
    .setId("o1")
    .setStatus(new ObjectStatus())
    .setState(new OaasObjectState()
      .setVerIds(DSMap.of("k1", "kkkk"))
    );

  public static final OaasObject OBJ_2 = OaasObject.createFromClasses(CLS_1)
    .setId("o2")
    .setStatus(new ObjectStatus());

  public static MutableMap<String, OaasClass> testClasses() {
    var clsResolver = new ClassResolver(null);
    var cls1 = clsResolver.resolve(CLS_1.copy(), List.of());
    var cls2 = clsResolver.resolve(CLS_2.copy(), List.of(cls1));
    return Lists.fixedSize.of(
        cls1,
        cls2
      )
      .groupByUniqueKey(OaasClass::getKey);
  }

  public static MutableMap<String, OaasFunction> testFunctions() {
    return Lists.fixedSize.of(
        FUNC_NEW.copy(),
        FUNC_1.copy(),
        MACRO_FUNC_1.copy(),
        ATOMIC_MACRO_FUNC.copy()
      )
      .groupByUniqueKey(OaasFunction::getKey);
  }

  public static List<OaasObject> testObjects() {
    var l = Lists.mutable.<OaasObject>empty();
    l.add(OBJ_1.copy());
    l.add(OBJ_2.copy());
    return l;
  }

  public static List<InvocationNode> testNodes() {
    return List.of();
  }


  public static RepoContextLoader mockContextLoader(MutableMap<String, OaasObject> objects,
                                                    MutableMap<String, OaasClass> classes,
                                                    MutableMap<String, OaasFunction> functions,
                                                    MutableMap<String, InvocationNode> nodes) {

    var clsRepo =  new MapEntityRepository.MapClsRepository(classes);
    var funcRepo = new MapEntityRepository.MapFnRepository(functions);
    var nodeRepoManager = new MapEntityRepository.MapInvRepoManager(nodes, classes);
    var objectRepoManager =  new MapEntityRepository.MapObjectRepoManager(objects ,classes);
    return new RepoContextLoader(objectRepoManager, funcRepo, clsRepo, nodeRepoManager);
  }
  public static void persistMock(ObjectRepoManager objectRepoManager,
                                 ClassRepository clsRepo,
                                 FunctionRepository fnRepo) {
    for (OaasClass cls : testClasses()) {
      cls.validate();
      clsRepo.persist(cls);
    }
    for (OaasFunction func : testFunctions()) {
      func.validate(true);
      fnRepo.persist(func);
    }
    for (OaasObject testObject : testObjects()) {
      objectRepoManager.persistAsync(testObject)
        .await().indefinitely();
    }
  }

}
