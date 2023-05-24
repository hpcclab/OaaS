package org.hpcclab.oaas.model.invocation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record InvocationRequest(
  String target,
  String targetCls,
  String fb,
  Map<String, String> args,
  List<String> inputs,
  boolean immutable,
  boolean macro,
//  String function,
  String invId,
  String outId,
  Map<String,String> macroIds,
  @JsonIgnore String partKey,
  boolean loadOutput,
//  List<InvocationRequest> nextReq,
  long queTs
) {
  public static final String CE_TYPE = "oaas.invReq";
}
