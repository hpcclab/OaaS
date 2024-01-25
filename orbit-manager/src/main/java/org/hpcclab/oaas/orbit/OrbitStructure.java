package org.hpcclab.oaas.orbit;

import org.hpcclab.oaas.orbit.exception.OrbitDeployException;
import org.hpcclab.oaas.orbit.exception.OrbitUpdateException;
import org.hpcclab.oaas.orbit.optimize.OrbitDeploymentPlan;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.ProtoOClass;
import org.hpcclab.oaas.proto.ProtoOFunction;
import org.hpcclab.oaas.proto.ProtoOrbit;

import java.util.Set;

public interface OrbitStructure {
  long getId();
  Set<String> getAttachedCls();
  Set<String> getAttachedFn();
  void update(OrbitDeploymentPlan plan, DeploymentUnit unit);

  OrbitDeploymentPlan createPlan(DeploymentUnit unit);
  default void deployAll(OrbitDeploymentPlan plan, DeploymentUnit unit) throws OrbitDeployException {
    deployShared(plan);
    deployDataModule(plan);
    deployExecutionModule(plan);
    deployObjectModule(plan, unit);
    for (ProtoOFunction fn : unit.getFnListList()) {
      deployFunction(plan, fn);
    }
  }

  void deployShared(OrbitDeploymentPlan plan) throws OrbitDeployException;
  void deployObjectModule(OrbitDeploymentPlan plan,
                          DeploymentUnit unit) throws OrbitDeployException;
  void deployExecutionModule(OrbitDeploymentPlan plan) throws OrbitDeployException;
  void deployDataModule(OrbitDeploymentPlan plan) throws OrbitDeployException;
  void deployFunction(OrbitDeploymentPlan plan,
                      ProtoOFunction function) throws OrbitDeployException;

  void detach(ProtoOClass cls) throws OrbitUpdateException;
  void destroy() throws OrbitUpdateException;

  ProtoOrbit dump();
}
