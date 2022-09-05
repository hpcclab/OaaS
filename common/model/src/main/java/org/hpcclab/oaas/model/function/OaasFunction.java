package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.exception.OaasValidationException;
import org.hpcclab.oaas.model.function.OaasDataflow;
import org.hpcclab.oaas.model.function.OaasFunctionType;
import org.hpcclab.oaas.model.function.OaasFunctionValidation;
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
  @ProtoField(1)
  String name;
  @NotNull
  @ProtoField(2)
  OaasFunctionType type;
  @ProtoField(3)
  String outputCls;

  @ProtoField(4)
  OaasFunctionValidation validation;

  @ProtoField(5)
  OaasDataflow macro;

  @ProtoField(6)
  ProvisionConfig provision;

  @ProtoField(7)
  String description;

  public OaasFunction() {
  }

  @ProtoFactory
  public OaasFunction(String name,String description, OaasFunctionType type, String outputCls, OaasFunctionValidation validation, OaasDataflow macro, ProvisionConfig provision) {
    this.name = name;
    this.description = description;
    this.type = type;
    this.outputCls = outputCls;
    this.validation = validation;
    this.macro = macro;
    this.provision = provision;
  }

  public void validate() {
    if (provision!=null) provision.validate();
    if (type==OaasFunctionType.TASK) {
      macro = null;
    }
    if (type==OaasFunctionType.MACRO) {
      provision = null;
      if (macro==null) {
        throw new OaasValidationException(
          "Macro function('%s') must be defined 'macro' parameter".formatted(name)
        );
      }
    }
  }
}
