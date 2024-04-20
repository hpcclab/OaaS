package org.hpcclab.oaas.crm.optimize;

import lombok.Builder;

/**
 * @author Pawissanutt
 */

@Builder(toBuilder = true)
public record CrInstanceSpec(
  int minInstance,
  int maxInstance,
  String scaleDownDelay,
  int targetConcurrency,
  float requestsCpu,
  long requestsMemory,
  float limitsCpu,
  long limitsMemory,
  int minAvail,
  boolean enableHpa,
  boolean disable
) {
}
