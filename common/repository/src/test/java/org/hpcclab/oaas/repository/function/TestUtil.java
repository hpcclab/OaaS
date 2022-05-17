package org.hpcclab.oaas.repository.function;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.EntityRepository;
import org.hpcclab.oaas.repository.impl.MapEntityRepository;
import org.hpcclab.oaas.repository.impl.OaasClassRepository;
import org.hpcclab.oaas.repository.impl.OaasFuncRepository;

import java.util.List;

public class TestUtil {


  public static RepoContextLoader mockContextLoader(MutableMap<String,OaasObject> objects,
                                                    MutableMap<String,OaasClass> classes,
                                                    MutableMap<String,OaasFunction> functions) {
    var objRepo = mockObjectRepo(objects);
    var clsRepo = mockClsRepo(classes);
    var funcRepo = mockFuncRepo(functions);
    var cl = new RepoContextLoader(objRepo,funcRepo,clsRepo);
    return cl;
  }

  public static EntityRepository<String, OaasObject> mockObjectRepo(MutableMap<String,OaasObject> objects) {
    return new MapEntityRepository<>(objects, OaasObject::getId);
  }


  public static EntityRepository<String, OaasClass> mockClsRepo(MutableMap<String,OaasClass> classes) {
    return new MapEntityRepository<>(classes, OaasClass::getName);
  }

  public static EntityRepository<String, OaasFunction> mockFuncRepo(MutableMap<String,OaasFunction> functions) {
    return new MapEntityRepository<>(functions, OaasFunction::getName);
  }


}
