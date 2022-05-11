package org.hpcclab.oaas.model.cls;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.cls.ReferenceSpecification;
import org.hpcclab.oaas.model.exception.OaasValidationException;
import org.hpcclab.oaas.model.function.OaasFunctionBinding;
import org.hpcclab.oaas.model.object.ObjectType;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.model.state.StateSpecification;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Set;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasClass {
  @ProtoField(1)
  String name;
  @ProtoField(2)
  String genericType;
  @ProtoField(3)
  ObjectType objectType;
  @ProtoField(4)
  OaasObjectState.StateType stateType;
  @ProtoField(5)
  Set<OaasFunctionBinding> functions;
  @ProtoField(6)
  StateSpecification stateSpec;
  @ProtoField(7)
  Set<ReferenceSpecification> refSpec;
  @ProtoField(8)
  Set<String> parents;

  public OaasClass() {
  }

  @ProtoFactory
  public OaasClass(String name, String genericType, ObjectType objectType, OaasObjectState.StateType stateType, Set<OaasFunctionBinding> functions, StateSpecification stateSpec, Set<ReferenceSpecification> refSpec, Set<String> parents) {
    this.name = name;
    this.genericType = genericType;
    this.objectType = objectType;
    this.stateType = stateType;
    this.functions = functions;
    this.stateSpec = stateSpec;
    this.refSpec = refSpec;
    this.parents = parents;
  }

  public void validate() {
    if (stateSpec==null) stateSpec = new StateSpecification();
    stateSpec.validate();
    if (stateType==OaasObjectState.StateType.COLLECTION
      && stateSpec.getDefaultProvider()==null) {
      throw new OaasValidationException("Class with COLLECTION type must define 'stateSpec.defaultProvider'");
    }
    for (OaasFunctionBinding function : functions) {
      if (function.getFunction() == null) {
        throw new OaasValidationException("The 'functions.function' in class must not be null.");
      }
      if (function.getName() == null) {
        var fullFuncName = function.getFunction();
        var i = fullFuncName.lastIndexOf('.');
        if (i < 0) function.setName(fullFuncName);
        else function.setName(fullFuncName.substring(i+1));
      }
    }
  }
}
