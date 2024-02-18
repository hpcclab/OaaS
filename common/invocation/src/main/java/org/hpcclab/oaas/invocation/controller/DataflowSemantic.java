package org.hpcclab.oaas.invocation.controller;

import com.google.common.base.Objects;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.hpcclab.oaas.model.function.DataflowStep;
import org.hpcclab.oaas.model.function.MacroSpec;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Pawissanutt
 */
public class DataflowSemantic {
  MacroSpec macroSpec;
  DataflowNode rootNode;
  List<DataflowNode> allNode;
  DataflowNode exportNode;
  Map<String, DataflowNode> exportNodes;

  protected DataflowSemantic(MacroSpec macroSpec,
                             DataflowNode rootNode) {
    this.macroSpec = macroSpec;
    this.rootNode = rootNode;
    this.allNode = Lists.mutable.empty();
  }

  public static DataflowSemantic construct(MacroSpec spec) {
    var root = new DataflowNode();
    var df = new DataflowSemantic(spec, root);
    var steps = spec.getSteps();
    MutableMap<String, DataflowNode> stateMap = Maps.mutable.of("$", root);
    for (int i = 0; i < steps.size(); i++) {
      var step = steps.get(i);
      DataflowNode targetNode = resolve(stateMap, step.getTarget());
      List<DataflowNode> inputNodes = step.getInputRefs().stream()
        .map(ref -> resolve(stateMap, ref))
        .toList();
      MutableSet<DataflowNode> require = Sets.mutable.empty();
      require.add(targetNode);
      require.addAll(inputNodes);
      var node = new DataflowNode(i, step, require, Sets.mutable.empty());
      df.allNode.add(node);
      if (step.getAs() != null && !step.getAs().isEmpty()) {
        stateMap.put(step.getAs(), node);
      }
    }
    for (DataflowNode node : df.allNode) {
      node.simplifyRequire();
      node.markNext(null);
      if (node.require.isEmpty()) {
        root.next.add(node);
      }
    }
    df.exportNode = resolve(stateMap, spec.getExport());
    df.exportNodes = spec.getExports().stream()
      .map(ex -> Map.entry(ex.getAs(), resolve(stateMap, ex.getFrom())))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return df;
  }

  static DataflowNode resolve(Map<String, DataflowNode> stateMap, String ref) {
    var target = extractTargetInFlow(ref);
    var node = stateMap.get(target);
    if (node==null) throw new DataflowParseException("Cannot resolve ref("+ref+")", 500);
    return node;
  }

  static String extractTargetInFlow(String exp) {
    String[] split = exp.split("\\.");
    return split[0];
  }

  public static class DataflowNode {
    int stepIndex = -1;
    DataflowStep step;
    MutableSet<DataflowNode> require;
    MutableSet<DataflowNode> next;

    public DataflowNode() {
      this.require = Sets.mutable.empty();
      this.next = Sets.mutable.empty();
    }

    public DataflowNode(int stepIndex,
                        DataflowStep step,
                        MutableSet<DataflowNode> require,
                        MutableSet<DataflowNode> next) {
      this.stepIndex = stepIndex;
      this.step = step;
      this.require = require;
      this.next = next;
    }

    Set<DataflowNode> simplifyRequire() {
      if (require.isEmpty()) return Set.of();
      if (require.size() == 1) return require.getOnly().simplifyRequire();
      Set<DataflowNode> transRequireNodes = require
        .stream()
        .flatMap(node -> node.simplifyRequire().stream())
        .collect(Collectors.toSet());
      require.removeAll(transRequireNodes);
      transRequireNodes.addAll(require);
      return transRequireNodes;
    }

    void markNext(DataflowNode parent) {
      if (parent != null) next.add(parent);
      if (require.isEmpty()) return;
      for (DataflowNode node : require) {
        node.markNext(this);
      }
    }


    @Override
    public int hashCode() {
      return Objects.hashCode(stepIndex);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof DataflowNode node)
        return Objects.equal(stepIndex, node.stepIndex);
      return false;
    }
  }
}
