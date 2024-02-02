package org.hpcclab.oaas.crm.observe;

import org.hpcclab.oaas.crm.OprcComponent;

import java.util.Map;

public record CrPerformanceMetrics(
  Map<OprcComponent, SvcPerformanceMetrics> coreMetrics,
  Map<String, SvcPerformanceMetrics> fnMetrics) {

  public record SvcPerformanceMetrics(
    double cpu,
    long mem,
    double rps,
    double msLatency
  ){}
}
