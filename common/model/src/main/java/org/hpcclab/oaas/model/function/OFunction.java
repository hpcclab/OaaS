package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.Copyable;
import org.hpcclab.oaas.model.HasKey;
import org.hpcclab.oaas.model.SelfValidatable;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.provision.ProvisionConfig;
import org.hpcclab.oaas.model.qos.QosConstraint;
import org.hpcclab.oaas.model.qos.QosRequirement;

import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OFunction implements Copyable<OFunction>, HasKey<String>, SelfValidatable {
  @JsonProperty("_key")
  String key;
  String name;
  String pkg;
  String description;
  @NotNull
  FunctionType type;
  @NotBlank
  String outputCls;
  Dataflows.Spec macro;
  ProvisionConfig provision;
  List<VariableDescription> variableDescriptions;
  OFunctionDeploymentStatus status;
  FunctionState state = FunctionState.ENABLED;
  QosRequirement qos;
  QosConstraint constraint;
  OFunctionConfig config;
  boolean immutable;

  public OFunction() {
  }

  public OFunction(String name,
                   String pkg,
                   String description,
                   FunctionType type,
                   String outputCls,
                   Dataflows.Spec macro,
                   ProvisionConfig provision,
                   List<VariableDescription> variableDescriptions,
                   OFunctionDeploymentStatus status,
                   FunctionState state,
                   QosRequirement qos,
                   QosConstraint constraint,
                   OFunctionConfig config,
                   boolean immutable) {
    this.name = name;
    this.pkg = pkg;
    this.description = description;
    this.type = type;
    this.outputCls = outputCls;
    this.macro = macro;
    this.provision = provision;
    this.variableDescriptions = variableDescriptions;
    this.status = status;
    this.state = state;
    this.qos = qos;
    this.constraint = constraint;
    this.config = config;
    this.immutable = immutable;
    updateKey();
  }

  @Override
  public void validate() {
    if (name==null)
      throw new FunctionValidationException("Function's name can not be null");
    if (!name.matches("^[a-zA-Z0-9._-]*$"))
      throw new FunctionValidationException("Function's name must be follow the pattern of '^[a-zA-Z0-9._-]*$'");
    if (provision!=null) provision.validate();
    if (config==null) config = new OFunctionConfig();
    if (config.getOffloadingMode() == null)
      config.setOffloadingMode(OFunctionConfig.OffloadingMode.JSON);
    if (type==null)
      type = FunctionType.TASK;
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
    if (status==null)
      status = new OFunctionDeploymentStatus();
    if (type==FunctionType.MACRO || type==FunctionType.BUILTIN) {
      status.setCondition(DeploymentCondition.RUNNING);
    } else {
      status.setCondition(DeploymentCondition.PENDING);
    }

    if (outputCls!=null &&
      (outputCls.equalsIgnoreCase("none") ||
        outputCls.equalsIgnoreCase("void"))) {
      outputCls = null;
    }

    if (provision!=null && provision.getStaticUrl()!=null) {
      status.setCondition(DeploymentCondition.RUNNING)
        .setInvocationUrl(provision.getStaticUrl().getUrl());
    }

  }

  public OFunction setName(String name) {
    this.name = name;
    updateKey();
    return this;
  }

  public OFunction setPkg(String pkg) {
    this.pkg = pkg;
    updateKey();
    return this;
  }

  public void updateKey() {
    if (pkg!=null) {
      this.key = pkg + '.' + name;
      if (outputCls!=null && outputCls.startsWith("."))
        outputCls = pkg + outputCls;
    } else {
      this.key = name;
    }
  }

  @Override
  public OFunction copy() {
    return new OFunction(
      name,
      pkg,
      description,
      type,
      outputCls,
      macro,
      provision,
      variableDescriptions == null? null : List.copyOf(variableDescriptions),
      status,
      state,
      qos,
      constraint,
      config,
      immutable
    );
  }
}
