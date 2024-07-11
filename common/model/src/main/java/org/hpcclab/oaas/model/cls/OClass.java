package org.hpcclab.oaas.model.cls;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.Copyable;
import org.hpcclab.oaas.model.HasKey;
import org.hpcclab.oaas.model.SelfValidatable;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.exception.OaasValidationException;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.object.OObjectType;
import org.hpcclab.oaas.model.qos.ConsistencyModel;
import org.hpcclab.oaas.model.qos.QosConstraint;
import org.hpcclab.oaas.model.qos.QosRequirement;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.hpcclab.oaas.model.state.StateSpecification;
import org.hpcclab.oaas.model.state.StateType;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@AllArgsConstructor
@Builder(toBuilder = true)
public class OClass implements Copyable<OClass>, HasKey<String>, SelfValidatable {

  @JsonProperty("_key")
  String key;
  @JsonProperty("_rev")
  @JsonView(Views.Internal.class)
  String rev;
  String name;
  String pkg;
  OObjectType objectType;
  StateType stateType;
  List<FunctionBinding> functions = List.of();
  StateSpecification stateSpec;
  List<ReferenceSpecification> refSpec = List.of();
  List<String> parents = List.of();
  String description;
  boolean disabled;
  boolean markForRemoval;
  OClassConfig config;
  OClassDeploymentStatus status;
  @JsonAlias({"qos","requirement"})
  QosRequirement requirements;
  @JsonAlias({"constraint"})
  QosConstraint constraints;
  ResolvedMember resolved;


  public OClass() {
  }

  public OClass(String name,
                String pkg,
                OObjectType objectType,
                StateType stateType,
                List<FunctionBinding> functions,
                StateSpecification stateSpec,
                List<ReferenceSpecification> refSpec,
                List<String> parents,
                String description,
                boolean disabled,
                boolean markForRemoval,
                OClassConfig config,
                OClassDeploymentStatus status,
                QosRequirement requirements,
                QosConstraint constraints,
                ResolvedMember resolved) {
    this.name = name;
    this.pkg = pkg;
    this.objectType = objectType;
    this.stateType = stateType;
    this.functions = functions;
    this.stateSpec = stateSpec;
    this.refSpec = refSpec;
    this.parents = parents;
    this.description = description;
    this.disabled = disabled;
    this.markForRemoval = markForRemoval;
    this.config = config;
    this.status = status;
    this.requirements = requirements;
    this.constraints = constraints;
    this.resolved = resolved;
    updateKey();
  }

  public void validate() {
    if (name==null)
      throw new OaasValidationException("Class's name can not be null");
    if (!name.matches("^[a-zA-Z0-9._-]*$"))
      throw new OaasValidationException("Class's name must be follow the pattern of '^[a-zA-Z0-9._-]*$'");
    if (objectType==null) objectType = OObjectType.SIMPLE;
    if (stateType==null) stateType = StateType.FILES;
    if (stateSpec==null) stateSpec = new StateSpecification();
    stateSpec.validate();
    if (stateSpec.getDefaultProvider()==null) {
      stateSpec.setDefaultProvider("s3");
    }
    if (functions==null) functions = List.of();
    if (config==null) {
      config = new OClassConfig();
    }
    if (constraints== null)
      constraints = QosConstraint.builder()
        .consistency(ConsistencyModel.NONE)
        .build();
    config.validate();
  }


  @Override
  public OClass copy() {
    return new OClass(
      name,
      pkg,
      objectType,
      stateType,
      List.copyOf(functions),
      stateSpec==null ? null:stateSpec.copy(),
      refSpec==null ? null:List.copyOf(refSpec),
      parents==null ? null:List.copyOf(parents),
      description,
      markForRemoval,
      disabled,
      config,
      status,
      requirements,
      constraints,
      resolved==null ? null:resolved.copy()
    );
  }

  public OClass setName(String name) {
    this.name = name;
    updateKey();
    return this;
  }

  public OClass setPkg(String pkg) {
    this.pkg = pkg;
    updateKey();
    return this;
  }

  public void updateKey() {
    if (pkg!=null) {
      this.key = pkg + '.' + name;
    } else {
      this.key = name;
    }
  }

  public ResolvedMember getResolved() {
    if (resolved==null) {
      var fb = Optional.ofNullable(getFunctions())
        .stream()
        .flatMap(List::stream)
        .collect(Collectors.toMap(FunctionBinding::getName, Function.identity()));
      var ks = Optional.ofNullable(getStateSpec().getKeySpecs())
        .stream()
        .flatMap(List::stream)
        .collect(Collectors.toMap(KeySpecification::getName, Function.identity()));
      var rs = Optional.ofNullable(getRefSpec())
        .stream()
        .flatMap(List::stream)
        .collect(Collectors.toMap(ReferenceSpecification::getName, Function.identity()));
      var i = Set.<String>of();
      resolved = new ResolvedMember(
        fb, ks, rs, i, false
      );
    }

    return resolved;
  }

  public StateSpecification getStateSpec() {
    if (stateSpec==null) stateSpec = new StateSpecification();
    return stateSpec;
  }
}
