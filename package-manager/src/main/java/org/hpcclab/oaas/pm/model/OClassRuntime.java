package org.hpcclab.oaas.pm.model;

import com.arangodb.serde.jackson.Key;
import com.github.f4b6a3.tsid.Tsid;
import lombok.Builder;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.OFunction;

import java.util.List;

@Builder(toBuilder = true)
public record OClassRuntime(
  long id,
  String type,
  List<OClass> attachedCls,
  List<OFunction> attachedFn,
  String namespace,
  OrbitState state,
  boolean deleted,
  long stabilizationTime
) {
  public static String toKey(long id) {
    return Tsid.from(id).toLowerCase();
  }

  @Key
  public String getKey() {
    return Tsid.from(id).toLowerCase();
  }

  public record OrbitState(String jsonDump) {
  }
}
