package org.hpcclab.oaas.model.cls;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.Copyable;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.exception.OaasValidationException;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.object.ObjectType;
import org.hpcclab.oaas.model.state.StateSpecification;
import org.hpcclab.oaas.model.state.StateType;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OaasClass implements Copyable<OaasClass> {

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
                   List<String> parents) {
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

    updateKey();
  }

  public void validate() {
    if (objectType==null) objectType = ObjectType.SIMPLE;
    if (stateType==null) stateType = StateType.FILES;
    if (stateSpec==null) stateSpec = new StateSpecification();
    stateSpec.validate();
    if (stateSpec.getDefaultProvider()==null) {
      stateSpec.setDefaultProvider("s3");
    }
    if (functions==null) functions = List.of();
    for (FunctionBinding function : functions) {
      if (function.getFunction()==null) {
        throw new OaasValidationException("The 'functions[].function' in class must not be null.");
      }
      if (function.getName()==null) {
        var fullFuncName = function.getFunction();
        var i = fullFuncName.lastIndexOf('.');
        if (i < 0) function.setName(fullFuncName);
        else function.setName(fullFuncName.substring(i + 1));
      }
    }
  }

  public FunctionBinding findFunction(String funcName) {
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
      parents==null ? null:List.copyOf(parents)
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

  public void updateKey(){
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
}
