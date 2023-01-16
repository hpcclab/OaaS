package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.Copyable;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.provision.ProvisionConfig;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasFunction implements Copyable<OaasFunction> {
  @JsonProperty("_key")
//  @JsonView(Views.Internal.class)
  String key;
  @NotBlank
  @ProtoField(1)
  String name;

  @ProtoField(2)
  String pkg;
  @ProtoField(3)
  String description;
  @NotNull
  @ProtoField(4)
  FunctionType type;
  @NotBlank
  @ProtoField(5)
  String outputCls;

  @ProtoField(6)
  FunctionValidation validation;

  @ProtoField(7)
  Dataflow macro;

  @ProtoField(8)
  ProvisionConfig provision;

  @ProtoField(9)
  List<VariableDescription> variableDescriptions;

  @ProtoField(10)
  FunctionDeploymentStatus deploymentStatus;

  @ProtoField(11)
  FunctionState state = FunctionState.ENABLED;


  public OaasFunction() {
  }

  @ProtoFactory
  public OaasFunction(String name,
                      String pkg,
                      String description,
                      FunctionType type,
                      String outputCls,
                      FunctionValidation validation,
                      Dataflow macro,
                      ProvisionConfig provision,
                      List<VariableDescription> variableDescriptions,
                      FunctionDeploymentStatus deploymentStatus,
                      FunctionState state) {
    this.name = name;
    this.pkg = pkg;
    this.description = description;
    this.type = type;
    this.outputCls = outputCls;
    this.validation = validation;
    this.macro = macro;
    this.provision = provision;
    this.variableDescriptions = variableDescriptions;
    this.deploymentStatus = deploymentStatus;
    this.state = state;
    updateKey();
  }

  public void validate() {
    if (name == null)
      throw new FunctionValidationException("Function's name can not be null");
    if (!name.matches("^[a-zA-Z0-9_-]*$"))
      throw new FunctionValidationException("Function's name must be follow the pattern of '^[a-zA-Z0-9_-]*$'");
    if (provision!=null) provision.validate();
    if (type==FunctionType.TASK) {
      macro = null;
    }
    if (type==FunctionType.MACRO) {
      provision = null;
      if (macro==null) {
        throw new FunctionValidationException(
          "Macro function('%s') must be defined 'macro' parameter".formatted(name)
        );
      }
    }
    deploymentStatus = new FunctionDeploymentStatus();
    deploymentStatus.setCondition(DeploymentCondition.PENDING);

    if (outputCls != null &&
      (outputCls.equalsIgnoreCase("none") ||
      outputCls.equalsIgnoreCase("void"))) {
      outputCls = null;
    }
  }

  public OaasFunction setName(String name) {
    this.name = name;
    updateKey();
    return this;
  }

  public OaasFunction setPkg(String pkg) {
    this.pkg = pkg;
    updateKey();
    return this;
  }

  public void updateKey(){
    if (pkg!=null) {
      this.key = pkg + '.' + name;
      if (outputCls!= null && outputCls.startsWith("."))
        outputCls = pkg + outputCls;
    } else {
      this.key = name;
    }
  }

  @Override
  public OaasFunction copy() {
    return new OaasFunction(
      name,
      pkg,
      description,
      type,
      outputCls,
      validation,
      macro,
      provision,
      variableDescriptions == null? null : List.copyOf(variableDescriptions),
      deploymentStatus,
      state
    );
  }
}
