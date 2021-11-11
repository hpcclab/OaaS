package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.object.OaasObjectOrigin;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FunctionCallRequest {
  UUID target;
  @NotBlank
  String functionName;
  Map<String, String> args;
  List<UUID> additionalInputs;

  public static FunctionCallRequest from(OaasObjectOrigin origin) {
    return new FunctionCallRequest()
      .setTarget(origin.getParentId())
      .setFunctionName(origin.getFuncName())
      .setArgs(origin.getArgs())
      .setAdditionalInputs(origin.getAdditionalInputs());
  }
}
