package org.hpcclab.oaas.model.provision;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ProvisionConfig {
  @ProtoField(1)
  KnativeProvision knative;
  @ProtoField(2)
  KDeploymentProvision deployment;
  @ProtoField(3)
  StaticUrlProvision staticUrl;

  public ProvisionConfig() {
  }

  @ProtoFactory
  public ProvisionConfig(KnativeProvision knative,
                         KDeploymentProvision deployment,
                         StaticUrlProvision staticUrl) {
    this.knative = knative;
    this.deployment = deployment;
    this.staticUrl = staticUrl;
  }


  public void validate() {
    var nonNullCounter = 0;
    if (knative != null) nonNullCounter ++;
    if (deployment != null) nonNullCounter ++;
    if (staticUrl != null) nonNullCounter ++;
    if (nonNullCounter > 1)
      throw FunctionValidationException.format("provision config must be declared only one option");
  }
}
