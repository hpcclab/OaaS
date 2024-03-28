package org.hpcclab.oaas.crm.observe;

import java.util.Map;

public interface CrMetricObserver {
  Map<String, CrPerformanceMetrics> observe();
}
