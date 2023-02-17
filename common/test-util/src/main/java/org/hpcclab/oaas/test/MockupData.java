package org.hpcclab.oaas.test;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.hpcclab.oaas.invocation.RepoContextLoader;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.function.*;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.object.ObjectOrigin;
import org.hpcclab.oaas.model.object.ObjectStatus;
import org.hpcclab.oaas.model.object.ObjectType;
import org.hpcclab.oaas.model.state.KeyAccessModifier;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.model.state.StateSpecification;
import org.hpcclab.oaas.repository.*;

import java.util.List;
import java.util.Map;

public class MockupData {

  public static final OaasFunction FUNC_1 = new OaasFunction()
    .setName("func1")
    .setPkg("ex")
    .setType(FunctionType.TASK)
    .setDeploymentStatus(new FunctionDeploymentStatus()
      .setCondition(DeploymentCondition.RUNNING)
      .setInvocationUrl("http://localhost:8080")
    )
    ;

  public static final OaasFunction FUNC_2 = new OaasFunction()
    .setName("im-fn")
    .setPkg("ex")
    .setType(FunctionType.IM_TASK)
    .setDeploymentStatus(new FunctionDeploymentStatus()
      .setCondition(DeploymentCondition.RUNNING)
      .setInvocationUrl("http://localhost:8080")
    )
    ;

  public static final OaasFunction MACRO_FUNC_1 = new OaasFunction()
    .setName("macroFunc1")
    .setPkg("ex")
    .setType(FunctionType.MACRO)
//    .setOutputCls("ex.cls1")
    .setMacro(new Dataflow()
      .setSteps(List.of(
        new DataflowStep()
          .setFunction("f1")
          .setTarget("$")
          .setArgRefs(Map.of("key1","arg1"))
          .setAs("tmp1")
          .setArgs(Map.of("STEP", "1")),
        new DataflowStep()
          .setFunction("f3")
          .setTarget("tmp1")
          .setAs("tmp2")
          .setArgs(Map.of("STEP", "2"))
      ))
      .setExport("tmp2")
    );

  public static final OaasClass CLS_1 = new OaasClass()
    .setName("cls1")
    .setPkg("ex")
    .setObjectType(ObjectType.SIMPLE)
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
        .setName("f1")
        .setFunction( FUNC_1.getKey())
        .setOutputCls("ex.cls1")
        .setDefaultArgs(Map.of("aa", "aa", "aaa", "aaa")),
      new FunctionBinding()
        .setName("func2")
        .setFunction( FUNC_1.getKey())
        .setOutputCls(null),
      new FunctionBinding()
        .setName("f3")
        .setFunction(FUNC_1.getKey())
        .setForceImmutable(true)
        .setOutputCls("ex.cls1"),
      new FunctionBinding()
        .setName(FUNC_2.getName())
        .setFunction(FUNC_2.getKey())
        .setOutputCls("ex.cls1"),
      new FunctionBinding()
        .setName(MACRO_FUNC_1.getName())
        .setFunction(MACRO_FUNC_1.getKey())
        .setOutputCls("ex.cls1")
    ));

  public static final OaasClass CLS_2 = new OaasClass()
    .setName("cls2")
    .setPkg("ex")
    .setObjectType(ObjectType.SIMPLE)
    .setParents(List.of(CLS_1.getKey()));

  public final static OaasObject OBJ_1 = OaasObject.createFromClasses(CLS_1)
    .setId("o1")
    .setOrigin(new ObjectOrigin())
    .setStatus(new ObjectStatus())
    .setState(new OaasObjectState()
      .setVerIds(Maps.mutable.of("k1", "kkkk"))
    );

  public final static OaasObject OBJ_2 = OaasObject.createFromClasses(CLS_1)
    .setId("o2")
    .setOrigin(new ObjectOrigin()
      .setParentId(OBJ_1.getId())
    .setFbName("f1")
    )
    .setStatus(new ObjectStatus());

  public static MutableMap<String,OaasClass> testClasses() {
    var clsResolver = new ClassResolver();
    var cls1 = clsResolver.resolve(CLS_1.copy(), List.of());
    var cls2 = clsResolver.resolve(CLS_2.copy(), List.of(cls1));
    return Lists.fixedSize.of(
        cls1,
        cls2
    )
      .groupByUniqueKey(OaasClass::getKey);
  }

  public static MutableMap<String,OaasFunction> testFunctions() {
    return Lists.fixedSize.of(
      FUNC_1.copy(),
      MACRO_FUNC_1.copy()
    )
      .groupByUniqueKey(OaasFunction::getKey);
  }

  public static List<OaasObject> testObjects() {
    var l = Lists.mutable.<OaasObject>empty();
    l.add(OBJ_1.copy());
    l.add(OBJ_2.copy());
    return l;
  }


  public static RepoContextLoader mockContextLoader(MutableMap<String,OaasObject> objects,
                                                    MutableMap<String,OaasClass> classes,
                                                    MutableMap<String,OaasFunction> functions) {
    var objRepo = mockObjectRepo(objects);
    var clsRepo = mockClsRepo(classes);
    var funcRepo = mockFuncRepo(functions);
    return new RepoContextLoader(objRepo,funcRepo,clsRepo);
  }

  public static EntityRepository<String, OaasObject> mockObjectRepo(MutableMap<String,OaasObject> objects) {
    return new MapEntityRepository<>(objects, OaasObject::getId);
  }


  public static EntityRepository<String, OaasClass> mockClsRepo(MutableMap<String,OaasClass> classes) {
    return new MapEntityRepository<>(classes, OaasClass::getKey);
  }

  public static EntityRepository<String, OaasFunction> mockFuncRepo(MutableMap<String,OaasFunction> functions) {
    return new MapEntityRepository<>(functions, OaasFunction::getKey);
  }

  public static void persistMock(ObjectRepository objectRepo,
                                 ClassRepository clsRepo,
                                 FunctionRepository fnRepo) {
    objectRepo.persistAsync(testObjects())
      .await().indefinitely();
    clsRepo.persistAsync(testClasses().values())
      .await().indefinitely();
    fnRepo.persistAsync(testFunctions().values())
      .await().indefinitely();
  }

}
