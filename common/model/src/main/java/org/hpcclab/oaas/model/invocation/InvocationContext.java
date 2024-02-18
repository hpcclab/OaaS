package org.hpcclab.oaas.model.invocation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.function.FunctionType;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.model.task.TaskCompletion;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
@ToString(
  exclude = {"parent", "dataflowGraph"}
)
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvocationContext implements RoutableTaskMeta {
  @JsonIgnore
  InvocationContext parent;
  OObject output;
  OObject main;
  Map<String, OObject> mainRefs;
  OFunction function;
  List<OObject> inputs = List.of();
  Map<String, String> args = Map.of();
  boolean immutable;
  InvocationNode node;
  OClass mainCls;
  OClass outputCls;
  List<OObject> subOutputs = Lists.mutable.empty();
  FunctionBinding fb;
  Map<String, OObject> workflowMap = Maps.mutable.empty();
  List<InvocationContext> subContexts = Lists.mutable.empty();
  TaskCompletion completion;
  InvocationRequest request;
  @JsonIgnore
  DataflowGraph dataflowGraph;
  Map<String, OClass> clsMap = Map.of();
  ObjectNode respBody;

  long mqOffset = -1;


  public void addTaskOutput(OObject object) {
    if (object==null) return;
    subOutputs.add(object);
    if (parent!=null) {
      parent.addTaskOutput(object);
    }
  }

  @Override
  public String getFbName() {
    return fb.getName();
  }

  public void addSubContext(InvocationContext ctx) {
    subContexts.add(ctx);
    if (parent!=null) {
      parent.addSubContext(ctx);
    }
  }

  public boolean contains(InvocationContext taskContext) {
    var outId = getOutput().getId();
    if (taskContext.getOutput()!=null &&
      Objects.equals(taskContext.getOutput().getId(), outId))
      return true;
    for (InvocationContext subContext : subContexts) {
      if (subContext.contains(taskContext)) {
        return true;
      }
    }
    return false;
  }

  public OObject resolveDataflowRef(String ref) {
    if (ref.equals("$")) {
      return getMain();
    }
    if (ref.startsWith("$.")) {
      var refName = ref.substring(2);
      if (getMain().getRefs().containsKey(refName)) {
        var obj = getMainRefs().get(refName);
        if (obj!=null)
          return obj;
        else
          throw FunctionValidationException.cannotResolveMacro(ref,
            "object not found");
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



  public InvocationNode initNode() {
    if (node!=null)
      return node;
    node = new InvocationNode();
    if (request!=null) {
      node.setKey(request.invId());
      node.setOutId(output != null? output.getId(): null);
      node.setInputs(request.inputs());
    } else {
      node.setKey(getOutput().getId());
      node.setOutId(getOutput().getId());
      node.setInputs(getInputs().stream().map(OObject::getId).toList());
    }
    node.setFb(getFbName());
    node.setArgs(DSMap.copy(getArgs()));
    node.setMain(getMain().getKey());
    node.setCls(getMainCls().getKey());
    setNode(node);
    return node;
  }

  public InvocationRequest.InvocationRequestBuilder toRequest() {
    return initNode()
      .toReq()
      .immutable(getFb().isForceImmutable());
  }

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

  public InvocationResponse.InvocationResponseBuilder createResponse() {
    return InvocationResponse.builder()
      .invId(request.invId())
      .main(getMain())
      .output(getOutput())
      .fb(getFbName())
      .status(node==null ? null:node.getStatus())
      .body(respBody)
      .stats(node==null ? null:node.extractStats());
  }
}
