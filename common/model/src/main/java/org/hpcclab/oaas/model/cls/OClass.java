package org.hpcclab.oaas.model.cls;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.Copyable;
import org.hpcclab.oaas.model.HasKey;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.exception.OaasValidationException;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.object.OObjectType;
import org.hpcclab.oaas.model.qos.QosConstraint;
import org.hpcclab.oaas.model.qos.QosRequirement;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.hpcclab.oaas.model.state.StateSpecification;
import org.hpcclab.oaas.model.state.StateType;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Builder(toBuilder = true)
public class OClass implements Copyable<OClass>, HasKey<String> {

  @JsonProperty("_key")
  String key;
  @JsonProperty("_rev")
  @JsonView(Views.Internal.class)
  String rev;
  String name;
  String pkg;
  String genericType;
  OObjectType objectType;
  StateType stateType;
  List<FunctionBinding> functions = List.of();
  StateSpecification stateSpec;
  List<ReferenceSpecification> refSpec = List.of();
  List<String> parents = List.of();
  String description;
  boolean disabled;
  boolean markForRemoval;
  DatastoreLink store;
  OClassConfig config;
  OClassDeploymentStatus status;
  QosRequirement qos;
  QosConstraint constraint;
  ResolvedMember resolved;


  public OClass() {
  }

  public OClass(String name,
                String pkg,
                String genericType,
                OObjectType objectType,
                StateType stateType,
                List<FunctionBinding> functions,
                StateSpecification stateSpec,
                List<ReferenceSpecification> refSpec,
                List<String> parents,
                String description,
                boolean disabled,
                boolean markForRemoval,
                DatastoreLink store,
                OClassConfig config,
                OClassDeploymentStatus status,
                QosRequirement qos,
                QosConstraint constraint) {
    this.name = name;
    this.pkg = pkg;
    this.genericType = genericType;
    this.objectType = objectType;
    this.stateType = stateType;
    this.functions = functions;
    this.stateSpec = stateSpec;
    this.refSpec = refSpec;
    this.parents = parents;
    this.description = description;
    this.disabled = disabled;
    this.markForRemoval = markForRemoval;
    this.store = store;
    this.config = config;
    this.status = status;
    this.qos = qos;
    this.constraint = constraint;
    updateKey();
  }

  public OClass(String key, String rev, String name, String pkg, String genericType, OObjectType objectType, StateType stateType,
                List<FunctionBinding> functions, StateSpecification stateSpec, List<ReferenceSpecification> refSpec, List<String> parents,
                String description, boolean markForRemoval, boolean disabled,
                DatastoreLink store, OClassConfig config, OClassDeploymentStatus status, QosRequirement qos,
                QosConstraint constraint, ResolvedMember resolved) {
    this.key = key;
    this.rev = rev;
    this.name = name;
    this.pkg = pkg;
    this.genericType = genericType;
    this.objectType = objectType;
    this.stateType = stateType;
    this.functions = functions;
    this.stateSpec = stateSpec;
    this.refSpec = refSpec;
    this.parents = parents;
    this.description = description;
    this.markForRemoval = markForRemoval;
    this.disabled = disabled;
    this.store = store;
    this.config = config;
    this.status = status;
    this.qos = qos;
    this.constraint = constraint;
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
    config.validate();
  }

  public FunctionBinding findFunction(String funcName) {
    Objects.requireNonNull(funcName);
    var func = resolved.getFunctions()
      .get(funcName);
    if (func!=null) return func;
    return resolved
      .getFunctions()
      .values()
      .stream()
      .filter(fb -> funcName.equals(fb.getName()) || funcName.equals(fb.getFunction()))
      .findAny()
      .orElse(null);
  }

  @Override
  public OClass copy() {
    return new OClass(
      name,
      pkg,
      genericType,
      objectType,
      stateType,
      List.copyOf(functions),
      stateSpec==null ? null:stateSpec.copy(),
      refSpec==null ? null:List.copyOf(refSpec),
      parents==null ? null:List.copyOf(parents),
      description,
      markForRemoval,
      disabled,
      store,
      config,
      status,
      qos,
      constraint
    )
      .setResolved(resolved==null ? null:resolved.copy());
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

  @JsonIgnore
  public boolean isSamePackage(String classKey) {
    var i = classKey.lastIndexOf('.');
    if (i < 0)
      return getPkg()==null;
    var otherPkg = classKey.substring(0, i);
    return otherPkg.equals(pkg);
  }


  public Optional<ReferenceSpecification> findReference(String name) {
    return refSpec.stream()
      .filter(mem -> mem.getName().equals(name))
      .findFirst();
  }
}
