package org.hpcclab.oaas.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.function.FunctionAccessModifier;
import org.hpcclab.oaas.model.object.OaasObjectOrigin;
import org.hpcclab.oaas.model.proto.OaasClassPb;
import org.hpcclab.oaas.model.proto.OaasFunctionPb;
import org.hpcclab.oaas.model.proto.OaasObjectPb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FunctionExecContext {
  FunctionExecContext parent;
  OaasObjectPb main;
  OaasClassPb mainCls;
  OaasObjectPb entry;
  boolean reactive = true;
  OaasFunctionPb function;
  OaasClassPb outputCls;
  OaasObjectPb output;
  List<OaasObjectPb> taskOutputs = new ArrayList<>();
  FunctionAccessModifier functionAccess;
  Map<String, String> args = Map.of();
  List<OaasObjectPb> additionalInputs = List.of();
  Map<String, OaasObjectPb> workflowMap = Map.of();


  public OaasObjectOrigin createOrigin() {
    return new OaasObjectOrigin(
      main.getOrigin().getRootId(),
      main.getId(),
      function.getName(),
      args,
      additionalInputs.stream().map(OaasObjectPb::getId)
        .toList()
    );
  }

  public OaasObjectPb resolve(String ref) {
    return workflowMap.get(ref);
  }
}
