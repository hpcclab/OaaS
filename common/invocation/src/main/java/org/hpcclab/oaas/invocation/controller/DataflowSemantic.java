package org.hpcclab.oaas.invocation.controller;

import com.google.common.base.Objects;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.hpcclab.oaas.model.function.DataMapperDefinition;
import org.hpcclab.oaas.model.function.DataflowStep;
import org.hpcclab.oaas.model.function.MacroSpec;
import org.hpcclab.oaas.model.function.WorkflowExport;

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
    root.stepIndex = -1;
    var df = new DataflowSemantic(spec, root);
    var steps = spec.getSteps();
    MutableMap<String, DataflowNode> stateMap = Maps.mutable.of("$", root);
    for (int i = 0; i < steps.size(); i++) {
      var step = steps.get(i);
      DataflowNode targetNode = resolve(stateMap, step.target());
      List<DataMapperDefinition> inputRefs = step.inputDataMaps();
      if (inputRefs==null) inputRefs = List.of();
      List<DataflowNode> inputNodes = inputRefs.stream()
        .map(ref -> resolve(stateMap, ref.target()))
        .toList();
      MutableSet<DataflowNode> require = Sets.mutable.empty();
      require.add(targetNode);
      require.addAll(inputNodes);
      var node = new DataflowNode(i, step, require, Sets.mutable.empty());
      node.mainRefStepIndex = targetNode.stepIndex;
      df.allNode.add(node);
      if (step.as()!=null && !step.as().isEmpty()) {
        stateMap.put(step.as(), node);
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
    Set<WorkflowExport> exports = spec.getExports();
    if (exports ==null) exports = Set.of();
    df.exportNodes = exports.stream()
      .map(ex -> Map.entry(ex.getAs(), resolve(stateMap, ex.getFrom())))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return df;
  }

  static DataflowNode resolve(Map<String, DataflowNode> stateMap, String ref) {
    var target = extractTargetInFlow(ref);
    var node = stateMap.get(target);
    if (node==null) throw new DataflowParseException("Cannot resolve ref(" + ref + ")", 500);
    return node;
  }

  static String extractTargetInFlow(String exp) {
    String[] split = exp.split("\\.");
    return split[0];
  }

  public DataflowNode getRootNode() {
    return rootNode;
  }

  public MacroSpec getMacroSpec() {
    return macroSpec;
  }

  public List<DataflowNode> getAllNode() {
    return allNode;
  }

  public DataflowNode getExportNode() {
    return exportNode;
  }

  public Map<String, DataflowNode> getExportNodes() {
    return exportNodes;
  }

  public static class DataflowNode {
    int stepIndex = -1;
    DataflowStep step;
    MutableSet<DataflowNode> require;
    MutableSet<DataflowNode> next;
    int mainRefStepIndex = -2;

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
      if (require.size()==1) return require.getOnly().simplifyRequire();
      Set<DataflowNode> transRequireNodes = require
        .stream()
        .flatMap(node -> node.simplifyRequire().stream())
        .collect(Collectors.toSet());
      require.removeAll(transRequireNodes);
      transRequireNodes.addAll(require);
      return transRequireNodes;
    }

    void markNext(DataflowNode parent) {
      if (parent!=null) next.add(parent);
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

    public DataflowStep getStep() {
      return step;
    }

    public MutableSet<DataflowNode> getRequire() {
      return require;
    }

    public MutableSet<DataflowNode> getNext() {
      return next;
    }

    public int getStepIndex() {
      return stepIndex;
    }

    public int getMainRefStepIndex() {
      return mainRefStepIndex;
    }

    public boolean requireFanIn(){
      return require.size() > 1;
    }
  }
}
