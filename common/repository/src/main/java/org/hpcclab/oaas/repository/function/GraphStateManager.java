package org.hpcclab.oaas.repository.function;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.TaskContext;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.task.TaskCompletion;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface GraphStateManager {
  Uni<Collection<String>> getAllEdge(String srcId);
  default Uni<Collection<String>> getAllEdge(FunctionExecContext hint, String srcId){
    return getAllEdge(srcId);
  }
  Uni<Boolean> containEdge(String srcId, String desId);
  default Uni<Boolean> containEdge(FunctionExecContext hint, String srcId, String desId){
    return containEdge(srcId, desId);
  }
  Uni<Boolean> persistEdge(String srcId, String desId);
  Uni<Void> persistEdge(List<Map.Entry<String, String>> edgeMap);


  Multi<OaasObject> handleComplete(OaasObject completingObj);
  Multi<TaskContext> updateSubmitStatus(FunctionExecContext entryCtx, Collection<TaskContext> contexts);
}
