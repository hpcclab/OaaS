package org.hpcclab.oaas.crm.controller;

import com.github.f4b6a3.tsid.Tsid;
import org.hpcclab.oaas.crm.optimize.CrAdjustmentPlan;
import org.hpcclab.oaas.crm.optimize.CrDeploymentPlan;
import org.hpcclab.oaas.crm.optimize.QosOptimizer;
import org.hpcclab.oaas.proto.*;

import java.util.Map;

public interface CrController {
  long getId();

  default String getTsidString() {
    return Tsid.from(getId()).toLowerCase();
  }

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

  boolean doneInitialize();
  boolean isDeleted();

//  void updateStatus(String fnKey, ProtoOFunctionDeploymentStatus status);
}
