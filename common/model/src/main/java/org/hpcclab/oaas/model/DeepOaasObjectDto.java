package org.hpcclab.oaas.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.entity.object.OaasObject;
import org.hpcclab.oaas.entity.object.OaasObjectOrigin;
import org.hpcclab.oaas.entity.state.OaasObjectState;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeepOaasObjectDto {
  UUID id;
  OaasObjectOrigin origin;
  Long originHash;
  OaasObject.ObjectType type;
  OaasObject.AccessModifier access;
  DeepOaasClassDto cls;
  Map<String, String> labels;
  Set<DeepOaasFunctionBindingDto> functions = Set.of();
  OaasObjectState state;
  Set<OaasCompoundMemberDto> members;
}
