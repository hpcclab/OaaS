package org.hpcclab.msc.object.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.msc.object.entity.function.OaasFunction;
import org.hpcclab.msc.object.entity.function.OaasFunctionBinding;
import org.hpcclab.msc.object.entity.object.OaasObject;

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
  Map<String, String> args= Map.of();
  List<OaasObject> additionalInputs = List.of();
}
