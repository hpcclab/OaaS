package org.hpcclab.oaas.model.oal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import org.hpcclab.oaas.model.invocation.InvocationStats;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.task.TaskStatus;

import java.util.Map;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record OalResponse(
  OaasObject main,
  OaasObject output,
  String invId,
  String fbName,
  Map<String, String> macroIds,
  TaskStatus status,
  @JsonUnwrapped
  InvocationStats stats,
  boolean async,
  ObjectNode body
) {
}
