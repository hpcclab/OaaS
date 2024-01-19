package org.hpcclab.oaas.orbit;

import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.ProtoOrbit;

public interface OrbitTemplate {
  OrbitStructure create(DeploymentUnit deploymentUnit);
  OrbitStructure load(ProtoOrbit orbit);
  String type();
}
