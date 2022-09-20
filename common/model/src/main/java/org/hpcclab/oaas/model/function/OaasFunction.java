package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.exception.OaasValidationException;
import org.hpcclab.oaas.model.provision.ProvisionConfig;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasFunction {
  @JsonProperty("_key")
  @JsonView(Views.Internal.class)
  String key;
  @NotBlank
  @ProtoField(1)
  String name;
  @ProtoField(2)
  String description;
  @NotNull
  @ProtoField(3)
  FunctionType type;
  @ProtoField(4)
  String outputCls;

  @ProtoField(5)
  FunctionValidation validation;

  @ProtoField(6)
  Dataflow macro;

  @ProtoField(7)
  ProvisionConfig provision;

  @ProtoField(8)
  List<VariableDescription> variableDescriptions;



  public OaasFunction() {
  }

  @ProtoFactory
  public OaasFunction(String name, String description, FunctionType type, String outputCls, FunctionValidation validation, Dataflow macro, ProvisionConfig provision, List<VariableDescription> variableDescriptions) {
    this.name = name;
    this.key = name;
    this.description = description;
    this.type = type;
    this.outputCls = outputCls;
    this.validation = validation;
    this.macro = macro;
    this.provision = provision;
    this.variableDescriptions = variableDescriptions;
  }

  public void validate() {
    if (provision!=null) provision.validate();
    if (type==FunctionType.TASK) {
      macro = null;
    }
    if (type==FunctionType.MACRO) {
      provision = null;
      if (macro==null) {
        throw new OaasValidationException(
          "Macro function('%s') must be defined 'macro' parameter".formatted(name)
        );
      }
    }
  }

  public OaasFunction setName(String name) {
    this.name = name;
    this.key = name;
    return this;
  }
}
