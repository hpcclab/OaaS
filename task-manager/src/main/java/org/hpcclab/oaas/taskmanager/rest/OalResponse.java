package org.hpcclab.oaas.taskmanager.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import org.hpcclab.oaas.model.object.OaasObject;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record OalResponse(
  OaasObject target,
  OaasObject output,
  String invId,
  String fbName,
  boolean async
) {
}
