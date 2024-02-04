package org.hpcclab.oaas.arango;

import org.hpcclab.oaas.arango.repo.ArgClsRepository;
import org.hpcclab.oaas.arango.repo.ArgFunctionRepository;
import org.hpcclab.oaas.arango.repo.ArgObjectRepository;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;


public class AutoRepoBuilder {
  private AutoRepoBuilder() {
  }

  public static ArgClsRepository clsRepository(){
    var confRegistry = DatastoreConfRegistry.getDefault();
    if (!confRegistry.getConfMap().containsKey("PKG")) {
      throw new IllegalStateException("Can not load database config for ClsRepo");
    }
    var fac = new RepoFactory(confRegistry.getConfMap().get("PKG"));
    fac.init();
    return fac.clsRepository();
  }


  public static ArgFunctionRepository funcRepository(){
    var confRegistry = DatastoreConfRegistry.getDefault();
    if (!confRegistry.getConfMap().containsKey("PKG")) {
      throw new IllegalStateException("Can not load database config for FuncRepo");
    }
    var fac = new RepoFactory(confRegistry.getConfMap().get("PKG"));
    fac.init();
    return fac.fnRepository();
  }

}
