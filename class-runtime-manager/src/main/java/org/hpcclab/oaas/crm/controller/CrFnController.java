package org.hpcclab.oaas.crm.controller;

import org.hpcclab.oaas.crm.optimize.CrAdjustmentPlan;
import org.hpcclab.oaas.crm.optimize.CrDeploymentPlan;
import org.hpcclab.oaas.crm.optimize.CrInstanceSpec;
import org.hpcclab.oaas.proto.ProtoOFunction;

import java.util.List;
import java.util.Map;

public interface CrFnController<T> {
  FnResourcePlan deployFunction(CrDeploymentPlan plan,
                                ProtoOFunction function);

  FnResourcePlan applyAdjustment(CrAdjustmentPlan plan);

  List<T> removeFunction(String fnKey);

  List<T> removeAllFunction();

  void init(CrController parentController);

  void updateStableTime(String key);

  long getStableTime(String key);

  Map<String, CrInstanceSpec> currentSpecs();
}
