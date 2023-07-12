package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.invocation.InvocationNode;
import org.hpcclab.oaas.model.object.OaasObject;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskContext implements TaskDetail {
  String vId;
  OaasObject output;
  OaasObject main;
  Map<String, OaasObject> mainRefs;
  OaasFunction function;
  String fbName;
  List<OaasObject> inputs = List.of();
  Map<String, String> args = Map.of();
  boolean immutable;
  InvocationNode node;

  public Map<String, String> resolveArgs(FunctionBinding binding) {
    var defaultArgs = binding.getDefaultArgs();
    if (args!=null && defaultArgs!=null) {
      var finalArgs = Maps.mutable.ofMap(defaultArgs);
      finalArgs.putAll(args);
      return finalArgs;
    } else if (args==null && defaultArgs!=null) {
      return defaultArgs;
    } else if (args!=null) {
      return args;
    }
    return Map.of();
  }

  @Override
  public String getFuncKey() {
    return function.getKey();
  }

}
