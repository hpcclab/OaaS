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
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.oal.OalResponse;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.proto.KvPair;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskDetail;
import org.hpcclab.oaas.model.task.TaskStatus;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
@ToString(
  callSuper = true,
  exclude = {"parent", "dataflowGraph"}
)
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvocationContext implements TaskDetail {
  @JsonIgnore
  InvocationContext parent;
  String vId;
  OaasObject output;
  OaasObject main;
  Map<String, OaasObject> mainRefs;
  OaasFunction function;
  List<OaasObject> inputs = List.of();
  Map<String, String> args = Map.of();
  boolean immutable;
  InvocationNode node;
  OaasClass mainCls;
  OaasClass outputCls;
  List<OaasObject> subOutputs = Lists.mutable.empty();
  FunctionBinding fb;
  Map<String, OaasObject> workflowMap = Maps.mutable.empty();
  List<InvocationContext> subContexts = Lists.mutable.empty();
  TaskCompletion completion;
  InvocationRequest request;
  @JsonIgnore
  DataflowGraph dataflowGraph;
  Map<String, OaasClass> clsMap = Map.of();
  ObjectNode respBody;

  long mqOffset = -1;


  public void addTaskOutput(OaasObject object) {
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

  public InvocationNode initNode() {
    var node = getNode();
    if (node!=null)
      return node;
    node = new InvocationNode();
    if (request!=null) {
      node.setKey(request.invId());
      node.setOutId(request.outId());
      node.setInputs(request.inputs());
    } else {
      node.setKey(getOutput().getId());
      node.setOutId(getOutput().getId());
      node.setInputs(getInputs().stream().map(OaasObject::getId).toList());
    }
    node.setFb(getFbName());
    node.setArgs(KvPair.fromMap(getArgs()));
    node.setMain(getMain().getId());
    node.setCls(getMainCls().getKey());
    setNode(node);
    return node;
  }

  public InvocationRequest toRequest() {
    var partKey = getMain()!=null ? getMain().getId():null;
    return InvocationRequest.builder()
      .invId(request!=null ? request.invId():getOutput().getId())
      .partKey(partKey)
      .macro(false)
      .args(getArgs())
      .inputs(getInputs().stream().map(OaasObject::getId).toList())
      .cls(getMain().getCls())
      .main(getMain().getId())
      .fb(getFbName())
      .outId(getOutput()!=null ? getOutput().getId():null)
      .immutable(getFb().isForceImmutable())
      .nodeExist(true)
      .queTs(System.currentTimeMillis())
      .build();
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

  public OalResponse.OalResponseBuilder createResponse() {
    return OalResponse.builder()
      .invId(request.invId())
      .main(getMain())
      .output(getOutput())
      .fb(getFbName())
      .status(node == null? null : TaskStatus.READY)
      .body(respBody)
      .stats(node == null? null : node.extractStats());
  }
}
