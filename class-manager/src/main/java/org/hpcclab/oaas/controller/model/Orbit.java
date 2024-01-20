package org.hpcclab.oaas.controller.model;

import com.arangodb.serde.jackson.Key;

import java.util.List;

public record Orbit (
  long id,
  String type,
  List<String> attachedCls,
  List<String> attachedFn,
  String namespace,
  OrbitState state
){
  @Key
  public String getKey() {
    return String.valueOf(id);
  }
  public record OrbitState(String jsonDump){}
}
