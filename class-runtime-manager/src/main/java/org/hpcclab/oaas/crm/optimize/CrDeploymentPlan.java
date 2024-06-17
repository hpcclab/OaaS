package org.hpcclab.oaas.crm.optimize;

import lombok.Builder;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.crm.CrComponent;

import java.util.Map;

@Builder(toBuilder = true)
public record CrDeploymentPlan(
  Map<CrComponent, CrInstanceSpec> coreInstances,
  Map<String, CrInstanceSpec> fnInstances,
  CrDataSpec dataSpec
) {

  public CrDeploymentPlan update(CrAdjustmentPlan adjustmentPlan) {
    var c = Maps.mutable.ofMap(coreInstances);
    var f = Maps.mutable.ofMap(fnInstances);
    c.putAll(adjustmentPlan.coreInstances());
    f.putAll(adjustmentPlan.fnInstances());
    return new CrDeploymentPlan(c,f, dataSpec);
  }
}
