package org.hpcclab.oaas.model.invocation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import org.hpcclab.oaas.model.invocation.InvocationStats;
import org.hpcclab.oaas.model.invocation.InvocationStatus;
import org.hpcclab.oaas.model.object.IOObject;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.model.object.POObject;

import java.util.List;
import java.util.Map;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record InvocationResponse(
  IOObject<?> main,
  IOObject<?> output,
  String invId,
  String fb,
  Map<String, String> macroIds,
  List<String> macroInvIds,
  InvocationStatus status,
  @JsonUnwrapped
  InvocationStats stats,
  boolean async,
  ObjectNode body
) {
}
