package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.object.OaasObjectOrigin;
import org.hpcclab.oaas.model.proto.OaasClass;
import org.hpcclab.oaas.model.proto.OaasFunction;
import org.hpcclab.oaas.model.proto.OaasObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FunctionExecContext {
  FunctionExecContext parent;
  OaasObject main;
  OaasClass mainCls;
  OaasObject entry;
//  boolean reactive = true;
  OaasFunction function;
  OaasClass outputCls;
  OaasObject output;
  List<OaasObject> taskOutputs = new ArrayList<>();
  OaasFunctionBinding binding;
//  FunctionAccessModifier functionAccess;
  Map<String, String> args = Map.of();
  List<OaasObject> additionalInputs = List.of();
  Map<String, OaasObject> workflowMap = Map.of();


  public OaasObjectOrigin createOrigin() {
    var finalArgs = binding.getDefaultArgs();
    if (finalArgs == null) {
      finalArgs = args;
    }
    else if (args != null) {
      finalArgs.putAll(args);
    }

    return new OaasObjectOrigin(
      main.getOrigin().getRootId(),
      main.getId(),
      binding.getName(),
      finalArgs,
      additionalInputs.stream().map(OaasObject::getId)
        .toList()
    );
  }

  public OaasObject resolve(String ref) {
    return workflowMap.get(ref);
  }
}
