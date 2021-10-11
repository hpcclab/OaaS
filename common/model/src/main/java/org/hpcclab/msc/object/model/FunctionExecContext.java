package org.hpcclab.msc.object.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.msc.object.entity.function.OaasFunction;
import org.hpcclab.msc.object.entity.object.OaasObject;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FunctionExecContext {
  OaasObject main;
  OaasObject compound;
  boolean reactive = true;
  Map<String, OaasObject> members = Map.of();
  Map<String, OaasFunction> subFunctions = Map.of();
  OaasFunction function;
  Map<String, String> args= Map.of();
  List<OaasObject> additionalInputs = List.of();
}
