package org.hpcclab.oaas.model.cls;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.function.DeepOaasFunctionBinding;
import org.hpcclab.oaas.model.object.OaasObjectType;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.model.state.StateSpecification;

import java.util.List;
import java.util.Set;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeepOaasClass {
  String name;
  OaasObjectType objectType;
  OaasObjectState.StateType stateType;
  Set<DeepOaasFunctionBinding> functions;
  StateSpecification stateSpec;
  Set<ReferenceSpecification> refSpec;
  List<String> parents;
}
