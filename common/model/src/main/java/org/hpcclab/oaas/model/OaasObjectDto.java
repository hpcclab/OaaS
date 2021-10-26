package org.hpcclab.oaas.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.entity.object.OaasObject;
import org.hpcclab.oaas.entity.object.OaasObjectOrigin;
import org.hpcclab.oaas.entity.state.OaasObjectState;

import java.util.*;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasObjectDto {
  UUID id;
  OaasObjectOrigin origin;
  Long originHash;
  OaasObject.ObjectType type;
  OaasObject.AccessModifier access = OaasObject.AccessModifier.PUBLIC;
  String cls;
  Map<String, String> labels;
  Set<OaasFunctionBindingDto> functions = Set.of();
  OaasObjectState state;
  Set<OaasCompoundMemberDto> members;
}
