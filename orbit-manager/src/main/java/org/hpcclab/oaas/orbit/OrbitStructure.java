package org.hpcclab.oaas.orbit;

import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.ProtoOClass;
import org.hpcclab.oaas.proto.ProtoOFunction;
import org.hpcclab.oaas.proto.ProtoOrbit;

import java.util.List;
import java.util.Set;

public interface OrbitStructure {
  long getId();
  Set<String> getAttachedCls();
  Set<String> getAttachedFn();
  void update(DeploymentUnit unit);
  default void deployAll() throws Throwable{
    deployShared();
    deployDataModule();
    deployExecutionModule();
    deployObjectModule();
  }

  void deployShared() throws Throwable;
  void deployObjectModule() throws Throwable;
  void deployExecutionModule() throws Throwable;
  void deployDataModule() throws Throwable;
  void deployFunction(ProtoOFunction function) throws Throwable;

  void detach(ProtoOClass cls) throws  Throwable;
  void destroy() throws Throwable;

  ProtoOrbit dump();
}
