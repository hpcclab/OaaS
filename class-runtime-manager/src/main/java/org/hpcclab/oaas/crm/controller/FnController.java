package org.hpcclab.oaas.crm.controller;

import io.fabric8.kubernetes.api.model.HasMetadata;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.crm.optimize.CrAdjustmentPlan;
import org.hpcclab.oaas.crm.optimize.CrDeploymentPlan;
import org.hpcclab.oaas.proto.OFunctionStatusUpdate;
import org.hpcclab.oaas.proto.ProtoOFunction;

import java.util.List;

public interface FnController {
  FnResourcePlan deployFunction(CrDeploymentPlan plan,
                                   ProtoOFunction function);
  FnResourcePlan applyAdjustment(CrAdjustmentPlan plan);
  List<HasMetadata> removeFunction(String fnKey);
  List<HasMetadata> removeAllFunction();

}
