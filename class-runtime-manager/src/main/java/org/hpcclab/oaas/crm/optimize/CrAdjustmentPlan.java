package org.hpcclab.oaas.crm.optimize;

import org.hpcclab.oaas.crm.OprcComponent;

import java.util.Map;

public record CrAdjustmentPlan(
  Map<OprcComponent, Integer> coreInstances,
  Map<String, Integer> fnInstances,
  boolean needAction
) {
}
