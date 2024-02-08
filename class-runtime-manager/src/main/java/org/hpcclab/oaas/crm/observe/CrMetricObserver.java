package org.hpcclab.oaas.crm.observe;

import com.github.f4b6a3.tsid.Tsid;
import org.hpcclab.oaas.crm.controller.CrController;

import java.util.Map;

public interface CrMetricObserver {
  Map<String, CrPerformanceMetrics> observe();
}
