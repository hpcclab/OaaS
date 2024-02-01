package org.hpcclab.oaas.crm;

import org.hpcclab.oaas.crm.controller.CrController;
import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.crm.optimize.QosOptimizer;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.ProtoCr;

public interface OrbitTemplate {
  CrController create(OprcEnvironment env, DeploymentUnit deploymentUnit);
  CrController load(OprcEnvironment env, ProtoCr orbit);
  String type();

  QosOptimizer getQosOptimizer();
}
