package org.hpcclab.oaas.crm.controller;

import com.github.f4b6a3.tsid.Tsid;
import org.hpcclab.oaas.crm.optimize.CrAdjustmentPlan;
import org.hpcclab.oaas.crm.optimize.CrDeploymentPlan;
import org.hpcclab.oaas.crm.template.CrTemplate;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.ProtoCr;
import org.hpcclab.oaas.proto.ProtoOClass;
import org.hpcclab.oaas.proto.ProtoOFunction;

import java.util.Map;

public interface CrController {
  long getId();
  default String getTsidString() {
    return Tsid.from(getId()).toLowerCase();
  }
  CrTemplate getTemplate();
  Map<String, ProtoOClass> getAttachedCls();
  Map<String,ProtoOFunction> getAttachedFn();
  CrDeploymentPlan currentPlan();
  CrOperation createUpdateOperation(CrDeploymentPlan plan, DeploymentUnit unit);
  CrOperation createDeployOperation(CrDeploymentPlan plan, DeploymentUnit unit);
  CrOperation createDetachOperation(ProtoOClass cls);
  CrOperation createDestroyOperation();
  CrOperation createAdjustmentOperation(CrAdjustmentPlan adjustmentPlan);
  long getStableTime(String name);
  ProtoCr dump();
  boolean isInitialized();
  boolean isDeleted();
}
