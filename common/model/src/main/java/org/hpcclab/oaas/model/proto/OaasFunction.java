package org.hpcclab.oaas.model.proto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.function.OaasFunctionType;
import org.hpcclab.oaas.model.function.OaasFunctionValidation;
import org.hpcclab.oaas.model.function.OaasWorkflow;
import org.hpcclab.oaas.model.provision.ProvisionConfig;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasFunction {
  @NotBlank
  String name;
  @NotNull
  OaasFunctionType type;
  String outputCls;

  OaasFunctionValidation validation;

  OaasWorkflow macro;

  ProvisionConfig provision;

  public OaasFunction() {
  }

  @ProtoFactory
  public OaasFunction(String name, OaasFunctionType type, String outputCls, OaasFunctionValidation validation, OaasWorkflow macro, ProvisionConfig provision) {
    this.name = name;
    this.type = type;
    this.outputCls = outputCls;
    this.validation = validation;
    this.macro = macro;
    this.provision = provision;
  }

  @ProtoField(1)
  public String getName() {
    return name;
  }

  @ProtoField(2)
  public OaasFunctionType getType() {
    return type;
  }

  @ProtoField(3)
  public String getOutputCls() {
    return outputCls;
  }

  @ProtoField(4)
  public OaasFunctionValidation getValidation() {
    return validation;
  }

  @ProtoField(5)
  public OaasWorkflow getMacro() {
    return macro;
  }

  @ProtoField(6)
  public ProvisionConfig getProvision() {
    return provision;
  }

  public void validate() {
    if (provision!=null) provision.validate();
    if (type==OaasFunctionType.TASK) {
      macro = null;
    }
    if (type==OaasFunctionType.MACRO) {
      provision = null;
      if (macro==null) {
        throw new NoStackException(
          "Macro function('%s') must be defined 'macro' parameter".formatted(name),
          400
        );
      }
    }
  }
}
