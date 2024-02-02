package org.hpcclab.oaas.crm.controller;

import org.hpcclab.oaas.crm.exception.CrDeployException;
import org.hpcclab.oaas.crm.exception.CrUpdateException;
import org.hpcclab.oaas.crm.optimize.CrDeploymentPlan;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.ProtoCr;
import org.hpcclab.oaas.proto.ProtoOClass;

import java.util.Set;

public interface CrController {
  long getId();

  Set<String> getAttachedCls();

  Set<String> getAttachedFn();

  CrOperation createUpdateOperation(CrDeploymentPlan plan, DeploymentUnit unit);

  CrDeploymentPlan createPlan(DeploymentUnit unit);

  CrOperation createDeployOperation(CrDeploymentPlan plan, DeploymentUnit unit)
    throws CrDeployException;

  CrOperation createDetachOperation(ProtoOClass cls) throws CrUpdateException;

  CrOperation createDestroyOperation() throws CrUpdateException;

  ProtoCr dump();
}
