package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.function.FunctionType;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.invocation.InvocationNode;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.task.TaskDetail;
import org.hpcclab.oaas.model.task.TaskStatus;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

//  public boolean analyzeDeps(List<Map.Entry<OaasObject, OaasObject>> waitForGraph, List<OaasObject> failDeps) {
//    if (output != null)
//      output.getStatus().initWaitFor();
//    int fails = failDeps.size();
//    Map<String, OaasObject> refs = Objects.requireNonNullElse(mainRefs, Map.of());
//    boolean completed = analyzeDeps(refs.values(), waitForGraph, failDeps);
//
//    completed &= analyzeDeps(inputs, waitForGraph, failDeps);
//
//    completed &= analyzeDeps(main, waitForGraph, failDeps);
//
//    if (!completed && fails < failDeps.size()) {
//      if (output!= null)
//        output.getStatus().setTaskStatus(TaskStatus.DEPENDENCY_FAILED);
//      return false;
//    }
//    return completed;
//  }
//
//  private boolean analyzeDeps(Collection<OaasObject> deps,
//                              List<Map.Entry<OaasObject, OaasObject>> waitForGraph,
//                              List<OaasObject> failDeps) {
//    boolean completed = true;
//    for (var o : deps) {
//      completed &= analyzeDeps(o, waitForGraph, failDeps);
//    }
//    return completed;
//  }

//  private boolean analyzeDeps(OaasObject dep,
//                              List<Map.Entry<OaasObject, OaasObject>> waitForGraph,
//                              List<OaasObject> failDeps) {
//    if (dep.isReadyToUsed()) return true;
//    var ts = dep.getStatus().getTaskStatus();
//    if (ts.isFailed()) {
//      failDeps.add(dep);
//    } else if (output != null) {
//      waitForGraph.add(Map.entry(dep, output));
//      if (output.getStatus().getWaitFor().isEmpty()) {
//        output.getStatus().setWaitFor(Lists.mutable.empty());
//      }
//      output.getStatus().getWaitFor().add(dep.getId());
//    }
//    return false;
//  }

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
