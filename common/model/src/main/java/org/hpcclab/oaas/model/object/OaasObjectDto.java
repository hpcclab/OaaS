package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.model.function.OaasFunctionBindingDto;

import java.util.*;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasObjectDto {
  UUID id;
  OaasObjectOrigin origin;
  Long originHash;
  ObjectAccessModifier access = ObjectAccessModifier.PUBLIC;
  String cls;
  Map<String, String> labels;
  Set<OaasFunctionBindingDto> functions = Set.of();
  OaasObjectState state;
  Set<OaasCompoundMemberDto> members;
}
