package org.hpcclab.oaas.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.entity.function.OaasFunction;
import org.hpcclab.oaas.entity.function.OaasFunctionBinding;
import org.hpcclab.oaas.entity.object.OaasObject;
import org.hpcclab.oaas.model.function.FunctionAccessModifier;
import org.hpcclab.oaas.model.object.OaasObjectOrigin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FunctionExecContext {
  FunctionExecContext parent;
  OaasObject main;
  OaasObject entry;
  boolean reactive = true;
  OaasFunction function;
  OaasObject output;
  List<OaasObject> taskOutputs = new ArrayList<>();
  FunctionAccessModifier functionAccess;
  Map<String, String> args= Map.of();
  List<OaasObject> additionalInputs = List.of();


  public OaasObjectOrigin createOrigin() {
    return new OaasObjectOrigin(
      main.getOrigin().getRootId(),
      main.getId(),
      function.getName(),
      args,
      additionalInputs.stream().map(OaasObject::getId)
        .toList()
    );
  }
}
