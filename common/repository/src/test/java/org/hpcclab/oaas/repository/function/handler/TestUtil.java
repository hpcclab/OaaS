package org.hpcclab.oaas.repository.function.handler;

import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.model.function.OaasDataflow;
import org.hpcclab.oaas.model.function.OaasFunctionType;
import org.hpcclab.oaas.model.object.OaasObjectType;
import org.hpcclab.oaas.model.proto.OaasClass;
import org.hpcclab.oaas.model.proto.OaasFunction;
import org.hpcclab.oaas.model.proto.OaasObject;
import org.hpcclab.oaas.repository.OaasClassRepository;
import org.hpcclab.oaas.repository.OaasFuncRepository;
import org.hpcclab.oaas.repository.OaasObjectRepository;

import java.util.List;

public class TestUtil {


  public static ContextLoader mockContextLoader(List<OaasObject> objects,
                                                List<OaasClass> classes,
                                                List<OaasFunction> functions) {
    var objRepo = mockObjectRepo(objects);
    var clsRepo = mockClsRepo(classes);
    var funcRepo = mockFuncRepo(functions);
    var cl = new ContextLoader();
    cl.clsRepo = clsRepo;
    cl.objectRepo = objRepo;
    cl.funcRepo = funcRepo;
    return cl;
  }

  public static OaasObjectRepository mockObjectRepo(List<OaasObject> objects) {
    var map = Lists.fixedSize.ofAll(objects)
      .groupByUniqueKey(OaasObject::getId);
    return new OaasObjectRepository() {
      @Override
      public OaasObject get(String key) {
        return map.get(key);
      }

      @Override
      public List<OaasObject> listByIds(List<String> ids) {
        return ids.stream()
          .map(map::get)
          .toList();
      }
    };
  }


  public static OaasClassRepository mockClsRepo(List<OaasClass> classes) {
    var map = Lists.fixedSize.ofAll(classes)
      .groupByUniqueKey(OaasClass::getName);
    return new OaasClassRepository() {
      @Override
      public OaasClass get(String key) {
        return map.get(key);
      }
    };
  }

  public static OaasFuncRepository mockFuncRepo(List<OaasFunction> functions) {
    var map = Lists.fixedSize.ofAll(functions)
      .groupByUniqueKey(OaasFunction::getName);
    return new OaasFuncRepository() {
      @Override
      public OaasFunction get(String key) {
        return map.get(key);
      }
    };
  }

  public static List<OaasClass> testClasses () {
    return List.of(
      new OaasClass()
        .setName("test1")
        .setObjectType(OaasObjectType.SIMPLE)
    );
  }
  public static List<OaasFunction> testFunctions () {
    return List.of(
      new OaasFunction()
        .setName("test1")
        .setType(OaasFunctionType.TASK)
        .setOutputCls("test")
        .setMacro(new OaasDataflow()
        )
    );
  }
}
