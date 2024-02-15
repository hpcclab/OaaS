package org.hpcclab.oaas.crm.controller;

import org.hpcclab.oaas.crm.optimize.CrAdjustmentPlan;
import org.hpcclab.oaas.crm.optimize.CrDeploymentPlan;
import org.hpcclab.oaas.crm.optimize.QosOptimizer;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.ProtoCr;
import org.hpcclab.oaas.proto.ProtoOClass;
import org.hpcclab.oaas.proto.ProtoOFunction;

import java.util.Map;

public interface CrController {
  long getId();

  Map<String, ProtoOClass> getAttachedCls();
  Map<String,ProtoOFunction> getAttachedFn();

  CrDeploymentPlan createDeploymentPlan(DeploymentUnit unit);
  CrDeploymentPlan currentPlan();

  CrOperation createUpdateOperation(CrDeploymentPlan plan, DeploymentUnit unit);

  CrOperation createDeployOperation(CrDeploymentPlan plan, DeploymentUnit unit);

  CrOperation createDetachOperation(ProtoOClass cls);

  CrOperation createDestroyOperation();

  CrOperation createAdjustmentOperation(CrAdjustmentPlan adjustmentPlan);

  ProtoCr dump();
  QosOptimizer getOptimizer();
  boolean isDeleted();
}
