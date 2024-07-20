package org.hpcclab.oaas.repository;

import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.pkg.OPackage;

/**
 * @author Pawissanutt
 */
public interface PackageDeployer {
  void deploy(OPackage pkg);
  void detach(OClass cls);
  void detach(OFunction fn);

}
