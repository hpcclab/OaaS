package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.cls.DeepOaasClassDto;
import org.hpcclab.oaas.model.function.DeepOaasFunctionBindingDto;
import org.hpcclab.oaas.model.state.OaasObjectState;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeepOaasObjectDto {
  UUID id;
  OaasObjectOrigin origin;
  Long originHash;
  ObjectAccessModifier access;
  DeepOaasClassDto cls;
  Map<String, String> labels;
  Set<DeepOaasFunctionBindingDto> functions = Set.of();
  OaasObjectState state;
  Set<OaasCompoundMemberDto> members;

  public DeepOaasFunctionBindingDto findFunction(String name) {
    return Stream.concat(functions.stream(), cls.getFunctions().stream())
      .filter(fb -> fb.getFunction().getName().equals(name))
      .findFirst().orElse(null);
  }
}
