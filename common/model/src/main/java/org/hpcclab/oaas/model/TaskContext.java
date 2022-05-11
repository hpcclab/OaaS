package org.hpcclab.oaas.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.object.OaasObject;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskContext {
  OaasObject output;
  OaasObject main;
  Map<String,OaasObject> mainRefs;
  OaasFunction function;
  List<OaasObject> inputs = List.of();
}
