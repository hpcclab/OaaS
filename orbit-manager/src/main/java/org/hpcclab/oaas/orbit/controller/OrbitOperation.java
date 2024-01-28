package org.hpcclab.oaas.orbit.controller;

import org.hpcclab.oaas.orbit.env.OprcEnvironment;
import org.hpcclab.oaas.orbit.exception.OrbitDeployException;

public interface OrbitOperation {
  void apply() throws OrbitDeployException;
  OprcEnvironment.EnvResource estimate();
  default void rollback() throws OrbitDeployException {
    throw new OrbitDeployException("Operation cannot rollback");
  }
}
