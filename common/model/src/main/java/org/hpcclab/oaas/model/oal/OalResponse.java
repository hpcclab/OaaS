package org.hpcclab.oaas.model.oal;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import org.hpcclab.oaas.model.object.OaasObject;

import java.util.Map;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record OalResponse(
  OaasObject target,
  OaasObject output,
  String invId,
  String fbName,
  Map<String, String> macroIds,
  boolean async
) {
}
