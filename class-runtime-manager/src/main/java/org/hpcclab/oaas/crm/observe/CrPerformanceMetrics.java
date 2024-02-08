package org.hpcclab.oaas.crm.observe;

import org.hpcclab.oaas.crm.OprcComponent;

import java.util.List;
import java.util.Map;

public record CrPerformanceMetrics(
  Map<OprcComponent, SvcPerformanceMetrics> coreMetrics,
  Map<String, SvcPerformanceMetrics> fnMetrics) {

  public record SvcPerformanceMetrics(
    List<DataPoint> cpu,
    List<DataPoint> mem,
    List<DataPoint> rps,
    List<DataPoint> msLatency
  ){}


  public record DataPoint(long timestamp, double value){}
}
