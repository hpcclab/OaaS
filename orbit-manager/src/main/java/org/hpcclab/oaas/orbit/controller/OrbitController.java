package org.hpcclab.oaas.orbit.controller;

import org.hpcclab.oaas.orbit.exception.OrbitDeployException;
import org.hpcclab.oaas.orbit.exception.OrbitUpdateException;
import org.hpcclab.oaas.orbit.optimize.OrbitDeploymentPlan;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.ProtoOClass;
import org.hpcclab.oaas.proto.ProtoOrbit;

import java.util.Set;

public interface OrbitController {
  long getId();
  Set<String> getAttachedCls();
  Set<String> getAttachedFn();
  OrbitOperation createUpdateOperation(OrbitDeploymentPlan plan, DeploymentUnit unit);
  OrbitDeploymentPlan createPlan(DeploymentUnit unit);
  OrbitOperation createDeployOperation(OrbitDeploymentPlan plan, DeploymentUnit unit)
    throws OrbitDeployException;
  OrbitOperation createDetachOperation(ProtoOClass cls) throws OrbitUpdateException;
  OrbitOperation createDestroyOperation() throws OrbitUpdateException;
  ProtoOrbit dump();
}
