package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.TaskContext;
import org.hpcclab.oaas.model.object.ObjectOrigin;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.object.OaasObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FunctionExecContext extends TaskContext {
  //  OaasObject main;
//  List<OaasObject> inputs = List.of();
//  OaasObject output;
//  OaasFunction function;
  FunctionExecContext parent;
  OaasClass mainCls;
  OaasObject entry;
  OaasClass outputCls;
  List<OaasObject> taskOutputs = new ArrayList<>();
  OaasFunctionBinding binding;
  Map<String, String> args = Map.of();
  Map<String, OaasObject> workflowMap = Map.of();

  public ObjectOrigin createOrigin() {
    var finalArgs = binding.getDefaultArgs();
    if (finalArgs == null) {
      finalArgs = args;
    }
    else if (args != null) {
      finalArgs.putAll(args);
    }

    return new ObjectOrigin(
      getMain().getOrigin().getRootId(),
      getMain().getId(),
      binding.getName(),
      finalArgs,
      getInputs().stream().map(OaasObject::getId)
        .toList()
    );
  }

  public OaasObject resolve(String ref) {
    return workflowMap.get(ref);
  }

  public void addTaskOutput(OaasObject object) {
    taskOutputs.add(object);
    if (parent != null) {
      parent.addTaskOutput(object);
    }
  }

  public void addTaskOutput(Collection<OaasObject> objects) {
    taskOutputs.addAll(objects);
    if (parent != null) {
      parent.addTaskOutput(objects);
    }
  }
}
