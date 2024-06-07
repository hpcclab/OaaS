package org.hpcclab.oaas.invocation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.invocation.controller.InvocationLog;
import org.hpcclab.oaas.invocation.controller.StateOperation;
import org.hpcclab.oaas.model.invocation.InvocationChain;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.model.object.POObject;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.model.task.TaskCompletion;

import java.util.List;
import java.util.Map;

/**
 * @author Pawissanutt
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvocationCtx {
  InvocationRequest request;
  POObject output;
  POObject main;
  Map<String, POObject> mainRefs;
  List<POObject> inputs = List.of();
  Map<String, String> args = Map.of();
  boolean immutable;
  List<String> macroInvIds = Lists.mutable.empty();
  Map<String, String> macroIds = Maps.mutable.empty();
  TaskCompletion completion;
  ObjectNode respBody;
  List<StateOperation> stateOperations = List.of();
  InvocationLog log;
  List<InvocationChain> chains = List.of();
  long mqOffset = -1;
  long initTime = -1;

  public InvocationLog initLog() {
    if (log!=null)
      return log;

    log = new InvocationLog();
    if (request!=null) {
      log.setKey(request.invId());
      log.setOutId(output!=null ? output.getKey():null);
    } else {
      log.setKey(getOutput().getKey());
      log.setOutId(getOutput().getKey());
    }
    log.setFb(request!=null ? request.fb():null);
    log.setArgs(DSMap.copy(getArgs()));
    log.setMain(getMain().getKey());
    log.setCls(request.cls());
    return log;
  }

  public InvocationResponse.InvocationResponseBuilder createResponse() {
    return InvocationResponse.builder()
      .invId(request.invId())
      .main(getMain())
      .output(getOutput())
      .fb(request.fb())
      .status(log==null ? null:log.getStatus())
      .body(respBody)
      .stats(log==null ? null:log.extractStats())
      .macroIds(macroIds)
      .macroInvIds(macroInvIds)
      ;
  }
}
