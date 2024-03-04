package org.hpcclab.oaas.crm.optimize;

import lombok.Builder;
import org.hpcclab.oaas.crm.OprcComponent;

import java.util.Map;

@Builder(toBuilder = true)
public record CrAdjustmentPlan(
  Map<OprcComponent, CrInstanceSpec> coreInstances,
  Map<String, CrInstanceSpec> fnInstances,
  boolean needAction
) {
}
