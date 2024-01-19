package org.hpcclab.oaas.controller.model;

import java.util.List;
import java.util.Map;

public record OrbitHash(long id, List<String> assignedCls, Map<String, HashTopology> hashTopologies){
  public String getKey() {
    return String.valueOf(id);
  }
  public record HashTopology(int numSegment, List<ApiAddress> segmentAddr) {}
  public record ApiAddress(String host, int port) {}
}
