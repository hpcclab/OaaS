package org.hpcclab.oaas.model.cls;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.Copyable;
import org.hpcclab.oaas.model.HasKey;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.exception.OaasValidationException;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.object.ObjectType;
import org.hpcclab.oaas.model.state.StateSpecification;
import org.hpcclab.oaas.model.state.StateType;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.List;
import java.util.Objects;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OaasClass implements Copyable<OaasClass>, HasKey<String> {

  @JsonProperty("_key")
  String key;

  @JsonProperty("_rev")
  @JsonView(Views.Internal.class)
  String rev;
  @ProtoField(1)
  String name;
  @ProtoField(2)
  String pkg;
  @ProtoField(3)
  String genericType;
  @ProtoField(4)
  ObjectType objectType;
  @ProtoField(5)
  StateType stateType;
  @ProtoField(6)
  List<FunctionBinding> functions = List.of();
  @ProtoField(7)
  StateSpecification stateSpec;
  @ProtoField(8)
  List<ReferenceSpecification> refSpec = List.of();
  @ProtoField(9)
  List<String> parents = List.of();
  @ProtoField(10)
  String description;
  @ProtoField(value = 11, defaultValue = "false")
  boolean markForRemoval;
  @ProtoField(12)
  DatastoreLink store;

  ResolvedMember resolved;


  public OaasClass() {
  }

  @ProtoFactory
  public OaasClass(String name,
                   String pkg,
                   String description,
                   String genericType,
                   ObjectType objectType,
                   StateType stateType,
                   List<FunctionBinding> functions,
                   StateSpecification stateSpec,
                   List<ReferenceSpecification> refSpec,
                   List<String> parents,
                   boolean markForRemoval,
                   DatastoreLink store) {
    this.name = name;
    this.pkg = pkg;
    this.description = description;
    this.genericType = genericType;
    this.objectType = objectType;
    this.stateType = stateType;
    this.functions = functions;
    this.stateSpec = stateSpec;
    this.refSpec = refSpec;
    this.parents = parents;
    this.markForRemoval = markForRemoval;
    this.store = store;

    updateKey();
  }

  public void validate() {
    if (name == null)
      throw new OaasValidationException("Class's name can not be null");
    if (!name.matches("^[a-zA-Z0-9._-]*$"))
      throw new OaasValidationException("Class's name must be follow the pattern of '^[a-zA-Z0-9._-]*$'");
    if (objectType==null) objectType = ObjectType.SIMPLE;
    if (stateType==null) stateType = StateType.FILES;
    if (stateSpec==null) stateSpec = new StateSpecification();
    stateSpec.validate();
    if (stateSpec.getDefaultProvider()==null) {
      stateSpec.setDefaultProvider("s3");
    }
    if (functions==null) functions = List.of();
    for (FunctionBinding binding : functions) {
      binding.validate();
    }
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
  public OaasClass copy() {
    return new OaasClass(
      name,
      pkg,
      description,
      genericType,
      objectType,
      stateType,
      List.copyOf(functions),
      stateSpec==null ? null:stateSpec.copy(),
      refSpec==null ? null:List.copyOf(refSpec),
      parents==null ? null:List.copyOf(parents),
      markForRemoval,
      store
    )
      .setResolved(resolved==null ? null:resolved.copy());
  }

  public OaasClass setName(String name) {
    this.name = name;
    updateKey();
    return this;
  }

  public OaasClass setPkg(String pkg) {
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
    if (resolved==null) resolved = new ResolvedMember();
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
      return getPkg() == null;
    var otherPkg = classKey.substring(0, i);
    return otherPkg.equals(pkg);
  }
}
