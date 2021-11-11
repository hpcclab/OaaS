package org.hpcclab.oaas.model.cls;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.function.DeepOaasFunctionBindingDto;
import org.hpcclab.oaas.model.object.OaasObjectType;
import org.hpcclab.oaas.model.state.OaasObjectState;

import java.util.Set;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeepOaasClassDto {
  String name;
  OaasObjectType objectType;
  OaasObjectState.StateType stateType;
  Set<DeepOaasFunctionBindingDto> functions;
}
