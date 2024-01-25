package org.hpcclab.oaas.controller.model;

import com.arangodb.serde.jackson.Key;
import com.github.f4b6a3.tsid.Tsid;

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
    return Tsid.from(id).toLowerCase();
  }

  public static String toKey(long id) {
    return Tsid.from(id).toLowerCase();
  }
  public record OrbitState(String jsonDump){}
}
