package org.hpcclab.oaas.crm.optimize;

import org.hpcclab.oaas.crm.OprcComponent;

import java.util.Map;

public record CrAdjustmentPlan(
  CrDeploymentPlan current,
  Map<OprcComponent, Integer> instances
) {
}
