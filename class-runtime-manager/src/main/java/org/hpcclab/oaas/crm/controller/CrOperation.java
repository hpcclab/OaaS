package org.hpcclab.oaas.crm.controller;

import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.crm.exception.CrDeployException;
import org.hpcclab.oaas.proto.OClassStatusUpdate;
import org.hpcclab.oaas.proto.OFunctionStatusUpdate;

import java.util.List;

public interface CrOperation {
  void apply() throws CrDeployException;
  OprcEnvironment.EnvResource estimate();
  default void rollback() throws CrDeployException {
    throw new CrDeployException("Operation cannot rollback");
  }
  default CrOperation merge(CrOperation operation) {
    throw new IllegalStateException();
  }
  default StateUpdateOperation stateUpdates() {
    return EMPTY;
  }
  StateUpdateOperation EMPTY = new StateUpdateOperation(List.of(), List.of());
  record StateUpdateOperation(
    List<OFunctionStatusUpdate> fnUpdates,
    List<OClassStatusUpdate> clsUpdates
  ){
  }
}
