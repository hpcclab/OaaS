package org.hpcclab.oaas.model.invocation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import org.hpcclab.oaas.model.object.GOObject;
import org.hpcclab.oaas.model.object.JsonBytes;

import java.util.List;
import java.util.Map;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record InvocationResponse(
  GOObject main,
  GOObject output,
  String invId,
  String fb,
  Map<String, String> macroIds,
  List<String> macroInvIds,
  InvocationStatus status,
  InvocationStats stats,
  @JsonIgnore boolean async,
  JsonBytes body
) {
}
