package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.task.TaskContext;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskDetail;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface GraphStateManager {
  Uni<? extends Collection<String>> getAllEdge(String srcId);
  Uni<Void> persistEdge(String srcId, String desId);
  Uni<Void> persistEdge(List<Map.Entry<String, String>> edgeMap);
  Multi<OaasObject> persistThenLoadNext(TaskDetail task, TaskCompletion completingObj);
  Multi<TaskContext> updateSubmittingStatus(InvocationContext entryCtx, Collection<TaskContext> contexts);

  Uni<Void> persistAll(InvocationContext ctx);
  Uni<Void> persistAll(InvocationContext ctx, List<OaasObject> objects);
}
