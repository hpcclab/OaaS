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
import java.util.Optional;
import java.util.Set;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasClass implements Copyable<OaasClass> {

  @JsonProperty("_key")
  @JsonView(Views.Internal.class)
  String key;
  @ProtoField(1)
  String name;
  @ProtoField(2)
  String genericType;
  @ProtoField(3)
  ObjectType objectType;
  @ProtoField(4)
  StateType stateType;
  @ProtoField(5)
  List<FunctionBinding> functions;
  @ProtoField(6)
  StateSpecification stateSpec;
  @ProtoField(7)
  List<ReferenceSpecification> refSpec;
  @ProtoField(8)
  List<String> parents;
  @ProtoField(9)
  String description;

  @JsonView(Views.Internal.class)
  ResolvedMember resolvedMember;

  public OaasClass() {
  }

  @ProtoFactory
  public OaasClass(String name, String description, String genericType, ObjectType objectType, StateType stateType, List<FunctionBinding> functions, StateSpecification stateSpec, List<ReferenceSpecification> refSpec, List<String> parents) {
    this.name = name;
    this.key = name;
    this.description = description;
    this.genericType = genericType;
    this.objectType = objectType;
    this.stateType = stateType;
    this.functions = functions;
    this.stateSpec = stateSpec;
    this.refSpec = refSpec;
    this.parents = parents;
  }

  public void validate() {
    if (stateType == null) stateType = StateType.FILES;
    if (stateSpec==null) stateSpec = new StateSpecification();
    stateSpec.validate();
    if (stateSpec.getDefaultProvider() == null) {
      stateSpec.setDefaultProvider("s3");
    }
//    if (stateType==StateType.COLLECTION
//      && stateSpec.getDefaultProvider()==null) {
//      throw new OaasValidationException("Class with COLLECTION type must define 'stateSpec.defaultProvider'");
//    }
    for (FunctionBinding function : functions) {
      if (function.getFunction() == null) {
        throw new OaasValidationException("The 'functions[].function' in class must not be null.");
      }
      if (function.getName() == null) {
        var fullFuncName = function.getFunction();
        var i = fullFuncName.lastIndexOf('.');
        if (i < 0) function.setName(fullFuncName);
        else function.setName(fullFuncName.substring(i+1));
      }
    }
  }

  public Optional<FunctionBinding> findFunction(String funcName){
    return getFunctions()
      .stream()
      .filter(fb -> funcName.equals(fb.getName()) || funcName.equals(fb.getFunction()))
      .findFirst();
  }

  @Override
  public OaasClass copy() {
    return new OaasClass(
      name,
      description,
      genericType,
      objectType,
      stateType,
      List.copyOf(functions),
      stateSpec.copy(),
      List.copyOf(refSpec),
      List.copyOf(parents)
    )
      .setResolvedMember(resolvedMember.copy());
  }

  public OaasClass setName(String name) {
    this.name = name;
    this.key = name;
    return this;
  }
}
