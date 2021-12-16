package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.object.OaasObjectRequirement;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasFunctionValidation implements Serializable {
  private OaasObjectRequirement bindingRequirement;
  private List<OaasObjectRequirement> additionalInputs = List.of();

  public OaasFunctionValidation() {
  }

  @ProtoFactory
  public OaasFunctionValidation(OaasObjectRequirement bindingRequirement,
                                List<OaasObjectRequirement> additionalInputs) {
    this.bindingRequirement = bindingRequirement;
    this.additionalInputs = additionalInputs;
  }

  @ProtoField(1)
  public OaasObjectRequirement getBindingRequirement() {
    return bindingRequirement;
  }

  @ProtoField(2)
  public List<OaasObjectRequirement> getAdditionalInputs() {
    return additionalInputs;
  }
}
