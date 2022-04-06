package org.hpcclab.oaas.model.proto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.cls.ReferenceSpecification;
import org.hpcclab.oaas.model.exception.OaasValidationException;
import org.hpcclab.oaas.model.function.OaasFunctionBinding;
import org.hpcclab.oaas.model.object.OaasObjectType;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.model.state.StateSpecification;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.List;
import java.util.Set;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasClass {
  private String name;
  private OaasObjectType objectType;
  private OaasObjectState.StateType stateType;
  private Set<OaasFunctionBinding> functions;
  private StateSpecification stateSpec;
  private Set<ReferenceSpecification> refSpec;
  private Set<String> parents;

  public OaasClass() {
  }

  @ProtoFactory
  public OaasClass(String name, OaasObjectType objectType, OaasObjectState.StateType stateType, Set<OaasFunctionBinding> functions, StateSpecification stateSpec, Set<ReferenceSpecification> refSpec, Set<String> parents) {
    this.name = name;
    this.objectType = objectType;
    this.stateType = stateType;
    this.functions = functions;
    this.stateSpec = stateSpec;
    this.refSpec = refSpec;
    this.parents = parents;
  }

  @ProtoField(1)
  public String getName() {
    return name;
  }

  @ProtoField(2)
  public OaasObjectType getObjectType() {
    return objectType;
  }

  @ProtoField(3)
  public OaasObjectState.StateType getStateType() {
    return stateType;
  }

  @ProtoField(4)
  public Set<OaasFunctionBinding> getFunctions() {
    return functions;
  }

  @ProtoField(5)
  public StateSpecification getStateSpec() {
    return stateSpec;
  }

  @ProtoField(6)
  public Set<String> getParents() {
    return parents;
  }

  @ProtoField(7)
  public Set<ReferenceSpecification> getRefSpec() {
    return refSpec;
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
