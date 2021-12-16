package org.hpcclab.oaas.model.proto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.hpcclab.oaas.model.object.OaasObjectType;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.model.state.StateSpecification;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Set;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasClassPb {
  private String name;
  private OaasObjectType objectType;
  private OaasObjectState.StateType stateType;
  private Set<OaasFunctionBindingPb> functions;
  private StateSpecification stateSpec;

  public OaasClassPb() {
  }

  @ProtoFactory
  public OaasClassPb(String name, OaasObjectType objectType, OaasObjectState.StateType stateType, Set<OaasFunctionBindingPb> functions, StateSpecification stateSpec) {
    this.name = name;
    this.objectType = objectType;
    this.stateType = stateType;
    this.functions = functions;
    this.stateSpec = stateSpec;
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
  public Set<OaasFunctionBindingPb> getFunctions() {
    return functions;
  }

  @ProtoField(5)
  public StateSpecification getStateSpec() {
    return stateSpec;
  }

  public void validate() {
    if (stateSpec==null) stateSpec = new StateSpecification();
  }
}
