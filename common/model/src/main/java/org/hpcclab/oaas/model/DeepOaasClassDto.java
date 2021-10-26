package org.hpcclab.oaas.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.entity.object.OaasObject;
import org.hpcclab.oaas.entity.state.OaasObjectState;

import java.util.List;
import java.util.Set;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeepOaasClassDto {
  String name;
  OaasObject.ObjectType objectType;
  OaasObjectState.StateType stateType;
  Set<DeepOaasFunctionBindingDto> functions;
}
