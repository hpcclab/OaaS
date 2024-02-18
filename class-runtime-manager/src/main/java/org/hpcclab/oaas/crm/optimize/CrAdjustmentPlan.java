package org.hpcclab.oaas.crm.optimize;

import org.hpcclab.oaas.crm.OprcComponent;

import java.util.Map;

public record CrAdjustmentPlan(
  Map<OprcComponent, CrInstanceSpec> coreInstances,
  Map<String, CrInstanceSpec> fnInstances,
  boolean needAction
) {
}
