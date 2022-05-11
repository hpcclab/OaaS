package org.hpcclab.oaas.repository.function;

import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.model.function.OaasFunctionBinding;
import org.hpcclab.oaas.model.function.OaasFunctionType;
import org.hpcclab.oaas.model.object.ObjectOrigin;
import org.hpcclab.oaas.model.object.ObjectType;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.OaasClassRepository;
import org.hpcclab.oaas.repository.OaasFuncRepository;
import org.hpcclab.oaas.repository.OaasObjectRepository;

import java.util.List;
import java.util.Set;

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
      public Uni<OaasObject> getAsync(String key) {
        return Uni.createFrom().item(get(key));
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
    o1.setOrigin(new ObjectOrigin().setRootId("o1"));
    return List.of(o1);
  }
}
