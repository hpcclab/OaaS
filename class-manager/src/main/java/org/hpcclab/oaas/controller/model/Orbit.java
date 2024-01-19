package org.hpcclab.oaas.controller.model;

import java.util.List;

public record Orbit (
  long id,
  String type,
  List<String> attachedCls,
  List<String> attachedFn,
  String namespace,
  OrbitState state
){
  public String getKey() {
    return String.valueOf(id);
  }
  public record OrbitState(String jsonDump){}
}
