package org.hpcclab.oaas.repository.function;

import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
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


  public static RepoContextLoader mockContextLoader(MutableMap<String,OaasObject> objects,
                                                    List<OaasClass> classes,
                                                    List<OaasFunction> functions) {
    var objRepo = mockObjectRepo(objects);
    var clsRepo = mockClsRepo(classes);
    var funcRepo = mockFuncRepo(functions);
    var cl = new RepoContextLoader(objRepo,funcRepo,clsRepo);
    return cl;
  }

  public static OaasObjectRepository mockObjectRepo(MutableMap<String,OaasObject> objects) {
    return new OaasObjectRepository() {
      @Override
      public OaasObject get(String key) {
        return objects.get(key);
      }

      @Override
      public Uni<OaasObject> getAsync(String key) {
        return Uni.createFrom().item(get(key));
      }

      @Override
      public List<OaasObject> listByIds(List<String> ids) {
        return ids.stream()
          .map(objects::get)
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


}
