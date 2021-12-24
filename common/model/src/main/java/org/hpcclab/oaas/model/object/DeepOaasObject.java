package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.cls.DeepOaasClass;
import org.hpcclab.oaas.model.state.OaasObjectState;

import java.util.Set;
import java.util.UUID;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeepOaasObject {
  UUID id;
  OaasObjectOrigin origin;
  Long originHash;
  ObjectAccessModifier access;
  DeepOaasClass cls;
//  Map<String, String> labels;
  Set<String> labels;
//  Set<DeepOaasFunctionBindingDto> functions = Set.of();
  OaasObjectState state;
  Set<OaasCompoundMember> members;
}
