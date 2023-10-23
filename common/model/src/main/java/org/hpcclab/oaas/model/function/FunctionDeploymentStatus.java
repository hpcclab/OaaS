package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FunctionDeploymentStatus {
  @ProtoField(1)
  DeploymentCondition condition;
  @ProtoField(2)
  String invocationUrl;
  @ProtoField(3)
  String errorMsg;

  public FunctionDeploymentStatus() {
  }

  @ProtoFactory
  public FunctionDeploymentStatus(DeploymentCondition condition, String invocationUrl, String errorMsg) {
    this.condition = condition;
    this.invocationUrl = invocationUrl;
    this.errorMsg = errorMsg;
  }
}
