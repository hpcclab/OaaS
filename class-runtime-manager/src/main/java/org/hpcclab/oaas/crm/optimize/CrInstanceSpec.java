package org.hpcclab.oaas.crm.optimize;

/**
 * @author Pawissanutt
 */
public record CrInstanceSpec(
  int minInstance,
  int maxInstance,
  String scaleDownDelay,
  int targetConcurrency
) {
}
