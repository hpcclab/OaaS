package org.hpcclab.oaas.orbit;

import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.ProtoOFunction;
import org.hpcclab.oaas.proto.ProtoOrbit;

import java.util.List;

public interface OrbitStructure {
  long getId();
  List<String> getAttachedCls();
  List<String> getAttachedFn();
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

  ProtoOrbit dump();
}
