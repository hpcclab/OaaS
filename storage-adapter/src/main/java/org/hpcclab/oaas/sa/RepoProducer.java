package org.hpcclab.oaas.sa;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.hpcclab.oaas.arango.RepoFactory;
import org.hpcclab.oaas.arango.repo.ArgClsRepository;
import org.hpcclab.oaas.arango.repo.ArgFunctionRepository;
import org.hpcclab.oaas.arango.repo.ArgObjectRepository;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.ClassResolver;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;

@ApplicationScoped
public class RepoProducer {
  DatastoreConfRegistry confRegistry = DatastoreConfRegistry.createDefault();

  @Produces
  @ApplicationScoped
  ArgClsRepository clsRepository(){
    if (!confRegistry.getConfMap().containsKey("PKG")) {
      throw new IllegalStateException("Can not load database config for ClsRepo");
    }
    var fac = new RepoFactory(confRegistry.getConfMap().get("PKG"));
    fac.init();
    return fac.clsRepository();
  }

  @Produces
  @ApplicationScoped
  ArgFunctionRepository funcRepository(){
    if (!confRegistry.getConfMap().containsKey("PKG")) {
      throw new IllegalStateException("Can not load database config for FuncRepo");
    }
    var fac = new RepoFactory(confRegistry.getConfMap().get("PKG"));
    fac.init();
    return fac.fnRepository();
  }

  @Produces
  @ApplicationScoped
  ArgObjectRepository objRepository(){
    if (!confRegistry.getConfMap().containsKey("OBJ")) {
      throw new IllegalStateException("Can not load database config for FuncRepo");
    }
    var fac = new RepoFactory(confRegistry.getConfMap().get("OBJ"));
    fac.init();
    return fac.objRepository();
  }

  @Produces
  @ApplicationScoped
  ClassResolver classResolver(ClassRepository classRepository) {
    return new ClassResolver(classRepository);
  }
}
