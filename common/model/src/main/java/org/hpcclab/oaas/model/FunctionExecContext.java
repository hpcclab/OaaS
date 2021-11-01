package org.hpcclab.oaas.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.entity.function.OaasFunction;
import org.hpcclab.oaas.entity.function.OaasFunctionBinding;
import org.hpcclab.oaas.entity.object.OaasObject;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FunctionExecContext {
  OaasObject main;
  OaasObject entry;
  boolean reactive = true;
  OaasFunction function;
  OaasFunctionBinding.AccessModifier functionAccess;
//  Map<String, String> args= Map.of();
  List<OaasObject> additionalInputs = List.of();
}
