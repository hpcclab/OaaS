package org.hpcclab.oaas.orbit;

import org.hpcclab.oaas.orbit.env.OprcEnvironment;
import org.hpcclab.oaas.orbit.optimize.QosOptimizer;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.ProtoOrbit;

public interface OrbitTemplate {
  OrbitStructure create(OprcEnvironment env, DeploymentUnit deploymentUnit);
  OrbitStructure load(OprcEnvironment env, ProtoOrbit orbit);
  String type();

  QosOptimizer getQosOptimizer();
}
