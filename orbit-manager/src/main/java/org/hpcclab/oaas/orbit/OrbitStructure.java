package org.hpcclab.oaas.orbit;

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
  void update(DeploymentUnit unit);

  OrbitDeploymentPlan createPlan(DeploymentUnit unit);
  default void deployAll(OrbitDeploymentPlan plan, DeploymentUnit unit) throws Throwable{
    deployShared(plan);
    deployDataModule(plan);
    deployExecutionModule(plan);
    deployObjectModule(plan, unit);
    for (ProtoOFunction fn : unit.getFnListList()) {
      deployFunction(plan, fn);
    }
  }

  void deployShared(OrbitDeploymentPlan plan) throws Throwable;
  void deployObjectModule(OrbitDeploymentPlan plan,
                          DeploymentUnit unit) throws Throwable;
  void deployExecutionModule(OrbitDeploymentPlan plan) throws Throwable;
  void deployDataModule(OrbitDeploymentPlan plan) throws Throwable;
  void deployFunction(OrbitDeploymentPlan plan,
                      ProtoOFunction function) throws Throwable;

  void detach(ProtoOClass cls) throws  Throwable;
  void destroy() throws Throwable;

  ProtoOrbit dump();
}
