package org.hpcclab.oaas.crm.optimize;

import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.crm.OprcComponent;

import java.util.Map;

public record CrDeploymentPlan(
  Map<OprcComponent, Integer> coreInstances,
  Map<String, Integer> fnInstances
) {

  public CrDeploymentPlan update(CrAdjustmentPlan adjustmentPlan) {
    var c = Maps.mutable.ofMap(coreInstances);
    var f = Maps.mutable.ofMap(fnInstances);
    c.putAll(adjustmentPlan.coreInstances());
    f.putAll(adjustmentPlan.fnInstances());
    return new CrDeploymentPlan(c,f);
  }
}
