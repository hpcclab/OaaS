package org.hpcclab.oaas.crm.controller;

import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.crm.exception.CrDeployException;

public interface OrbitOperation {
  void apply() throws CrDeployException;
  OprcEnvironment.EnvResource estimate();
  default void rollback() throws CrDeployException {
    throw new CrDeployException("Operation cannot rollback");
  }
}
