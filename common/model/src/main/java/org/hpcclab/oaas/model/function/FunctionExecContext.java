package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.task.TaskContext;
import org.hpcclab.oaas.model.object.ObjectOrigin;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.task.TaskCompletion;

import java.util.*;

@Getter
@Setter
@ToString(callSuper = true)
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FunctionExecContext extends TaskContext {
  @JsonIgnore
  FunctionExecContext parent;
  OaasClass mainCls;
  OaasObject entry;
  OaasClass outputCls;
  List<OaasObject> subOutputs = Lists.mutable.empty();
  FunctionBinding binding;
  Map<String, OaasObject> workflowMap = Maps.mutable.empty();
  List<FunctionExecContext> subContexts = Lists.mutable.empty();

  TaskCompletion completion;
  InvocationRequest request;

  public ObjectOrigin createOrigin() {
    var finalArgs = resolveArgs(binding);
    return new ObjectOrigin(
      getMain().getId(),
      getFbName(),
      finalArgs,
      getInputs().stream().map(OaasObject::getId)
        .toList()
    );
  }

//  public OaasObject resolve(String ref) {
//    return workflowMap.get(ref);
//  }

  public void addTaskOutput(OaasObject object) {
    if (object == null) return;
    subOutputs.add(object);
    if (parent != null) {
      parent.addTaskOutput(object);
    }
  }
//
//  public void addTaskOutput(Collection<OaasObject> objects) {
//    subOutputs.addAll(objects);
//    if (parent != null) {
//      parent.addTaskOutput(objects);
//    }
//  }

  @Override
  public String getFbName() {
    return super.getFbName() == null ? binding.getName() : getFbName();
  }

  public void addSubContext(FunctionExecContext ctx) {
    subContexts.add(ctx);
    if (parent != null) {
      parent.addSubContext(ctx);
    }
  }

  public boolean contains(TaskContext taskContext) {
    var outId = getOutput().getId();
    if (taskContext.getOutput().getId().equals(outId))
      return true;
    for (FunctionExecContext subContext : subContexts) {
      if (subContext.contains(taskContext)) {
        return true;
      }
    }
    return false;
  }
}
