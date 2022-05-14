package org.hpcclab.oaas.repository.function;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.function.FunctionExecContext;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface GraphStateManager {
  Uni<Collection<String>> getAllEdge(String srcId);
  Uni<Collection<String>> getAllEdge(FunctionExecContext hint, String srcId);
  Uni<Boolean> containEdge(String srcId, String desId);
  Uni<Boolean> containEdge(FunctionExecContext hint, String srcId, String desId);
  Uni<Void> persistEdge(String srcId, String desId);
  Uni<Void> persistEdge(List<Map.Entry<String, String>> edgeMap);
}
