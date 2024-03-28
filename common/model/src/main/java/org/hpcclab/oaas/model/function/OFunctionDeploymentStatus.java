package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OFunctionDeploymentStatus {
  DeploymentCondition condition;
  String invocationUrl;
  String errorMsg;
  long ts;

  public OFunctionDeploymentStatus() {
  }

  public OFunctionDeploymentStatus(DeploymentCondition condition, String invocationUrl, String errorMsg, long ts) {
    this.condition = condition;
    this.invocationUrl = invocationUrl;
    this.errorMsg = errorMsg;
    this.ts = ts;
  }
}
