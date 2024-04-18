package org.hpcclab.oaas.crm.template;

import org.hpcclab.oaas.crm.CrControllerManager;
import org.hpcclab.oaas.crm.CrtMappingConfig;
import org.hpcclab.oaas.crm.controller.CrController;
import org.hpcclab.oaas.crm.env.EnvironmentManager;
import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.crm.optimize.QosOptimizer;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.ProtoCr;

public interface ClassRuntimeTemplate {
  CrController create(OprcEnvironment.Config env, DeploymentUnit deploymentUnit);
  CrController load(OprcEnvironment.Config env, ProtoCr orbit);
  String name();
  String type();
  void init(CrControllerManager crControllerManager, EnvironmentManager environmentManager);
  QosOptimizer getQosOptimizer();
  CrtMappingConfig.CrtConfig getConfig();
}
