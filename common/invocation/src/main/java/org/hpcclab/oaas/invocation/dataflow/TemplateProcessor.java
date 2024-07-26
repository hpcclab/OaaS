package org.hpcclab.oaas.invocation.dataflow;

import com.fasterxml.jackson.databind.JsonNode;
import org.hpcclab.oaas.invocation.InvocationCtx;

import java.util.Map;
import java.util.Set;

/**
 * @author Pawissanutt
 */
public interface TemplateProcessor {
  Replacer<JsonNode> createReplacer(Map<String, DataflowSemantic.DataflowNode> stateMap,
                                    JsonNode template);

  Replacer<Map<String, String>> createMapReplacer(Map<String, DataflowSemantic.DataflowNode> stateMap,
                                                  Map<String, String> template);

  interface Replacer<T> {
    T getReplaceVal(InvocationCtx ctx, DataflowState state);
    default Set<Integer> getRequires(){
      return Set.of();
    }
  }
}
