package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.object.ObjectRequirement;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FunctionValidation implements Serializable {
  private ObjectRequirement bindingRequirement;
  private List<ObjectRequirement> inputs = List.of();

  public FunctionValidation() {
  }

  @ProtoFactory
  public FunctionValidation(ObjectRequirement bindingRequirement,
                            List<ObjectRequirement> inputs) {
    this.bindingRequirement = bindingRequirement;
    this.inputs = inputs;
  }

  @ProtoField(1)
  public ObjectRequirement getBindingRequirement() {
    return bindingRequirement;
  }

  @ProtoField(2)
  public List<ObjectRequirement> getInputs() {
    return inputs;
  }
}
