package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OFunctionDeploymentStatus {
  DeploymentCondition condition;
  String invocationUrl;
  String errorMsg;
  long ts;
}
