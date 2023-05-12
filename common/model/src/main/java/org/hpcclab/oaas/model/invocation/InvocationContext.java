package org.hpcclab.oaas.model.invocation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.object.ObjectOrigin;
import org.hpcclab.oaas.model.proto.KvPair;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskContext;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString(
  callSuper = true,
  exclude = {"parent", "dataflowGraph"}
)
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvocationContext extends TaskContext {
  @JsonIgnore
  InvocationContext parent;
  OaasClass mainCls;
  OaasObject entry;
  OaasClass outputCls;
  List<OaasObject> subOutputs = Lists.mutable.empty();
  FunctionBinding binding;
  Map<String, OaasObject> workflowMap = Maps.mutable.empty();
  List<InvocationContext> subContexts = Lists.mutable.empty();
  TaskCompletion completion;
  InvocationRequest request;
  @JsonIgnore
  DataflowGraph dataflowGraph;

  long mqOffset = -1;

  public ObjectOrigin createOrigin() {
    var finalArgs = resolveArgs(binding);
    return new ObjectOrigin(
      getMain().getId(),
      getFbName(),
      finalArgs.entrySet().stream().map(KvPair::new).collect(Collectors.toSet()),
      getInputs().stream().map(OaasObject::getId)
        .toList()
    );
  }

  public void addTaskOutput(OaasObject object) {
    if (object==null) return;
    subOutputs.add(object);
    if (parent!=null) {
      parent.addTaskOutput(object);
    }
  }

  @Override
  public String getFbName() {
    return super.getFbName()==null ? binding.getName():getFbName();
  }

  public void addSubContext(InvocationContext ctx) {
    subContexts.add(ctx);
    if (parent!=null) {
      parent.addSubContext(ctx);
    }
  }

  public boolean contains(TaskContext taskContext) {
    var outId = getOutput().getId();
    if (taskContext.getOutput().getId().equals(outId))
      return true;
    for (InvocationContext subContext : subContexts) {
      if (subContext.contains(taskContext)) {
        return true;
      }
    }
    return false;
  }

  public OaasObject resolveDataflowRef(String ref) {
    if (ref.equals("$")) {
      return getMain();
    }
    if (ref.startsWith("$.")) {
      var res = getMain().findReference(ref.substring(2));
      if (res.isPresent()) {
        var obj = getMainRefs().get(res.get().getName());
        if (obj!=null)
          return obj;
        else
          throw FunctionValidationException.cannotResolveMacro(ref, "object not found");
      }
    }
    if (ref.startsWith("#")) {
      try {
        var i = Integer.parseInt(ref.substring(1));
        if (i >= getInputs().size())
          throw FunctionValidationException.cannotResolveMacro(ref,
            "index out of range: >=" + getInputs().size());
        return getInputs().get(i);
      } catch (NumberFormatException ignored) {
        throw FunctionValidationException.cannotResolveMacro(ref, "Ref number parsing error");
      }
    }

    if (getWorkflowMap().containsKey(ref))
      return getWorkflowMap().get(ref);
    throw FunctionValidationException.cannotResolveMacro(ref, null);
  }
}
