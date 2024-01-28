package org.hpcclab.oaas.orbit;

import org.hpcclab.oaas.orbit.controller.OrbitController;
import org.hpcclab.oaas.orbit.env.OprcEnvironment;
import org.hpcclab.oaas.orbit.optimize.QosOptimizer;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.ProtoOrbit;

public interface OrbitTemplate {
  OrbitController create(OprcEnvironment env, DeploymentUnit deploymentUnit);
  OrbitController load(OprcEnvironment env, ProtoOrbit orbit);
  String type();

  QosOptimizer getQosOptimizer();
}
