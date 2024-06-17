package org.hpcclab.oaas.invocation.dataflow;

import com.google.common.base.Objects;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.MutableMap;
import org.hpcclab.oaas.invocation.transform.ODataTransformer;
import org.hpcclab.oaas.model.function.Dataflows;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Pawissanutt
 */
public class DataflowSemantic {
  Dataflows.Spec macroSpec;
  DataflowNode rootNode;
  List<DataflowNode> stepNodes;
  DataflowNode endNode;

  protected DataflowSemantic(Dataflows.Spec macroSpec,
                             DataflowNode rootNode) {
    this.macroSpec = macroSpec;
    this.rootNode = rootNode;
    this.stepNodes = Lists.mutable.empty();
  }



  public static DataflowSemantic construct(Dataflows.Spec spec) {
    var root = new DataflowNode(-1);
    var df = new DataflowSemantic(spec, root);
    var steps = spec.steps();
    MutableMap<String, DataflowNode> stateMap = Maps.mutable.of("@", root);
    for (int i = 0; i < steps.size(); i++) {
      var step = steps.get(i);
      var node = createNode(i, step, stateMap, root);
      df.stepNodes.add(node);
      if (step.as()!=null && !step.as().isEmpty()) {
        stateMap.put(step.as(), node);
      }
    }
    for (DataflowNode node : df.stepNodes) {
      node.simplifyRequire();
      node.markNext(null);
      if (node.require.isEmpty()) {
        root.next.add(node);
      }
    }
    var endStep = Dataflows.Step.builder()
      .mappings(spec.respBody())
      .target(spec.output())
      .build();
    df.endNode = createNode(steps.size(), endStep, stateMap, root);
    return df;
  }

  static DataflowNode createNode(int stepIndex,
                                 Dataflows.Step step,
                                 MutableMap<String, DataflowNode> stateMap,
                                 DataflowNode root) {
    List<Dataflows.DataMapping> mappings = step.mappings();
    if (mappings==null) mappings = List.of();
    var dmats = mappings.stream()
      .map(ref -> new DataMapAndTransformer(resolve(stateMap, ref), ref))
      .toList();
    Set<DataflowNode> require =
      dmats.stream().map(DataMapAndTransformer::node).collect(Collectors.toSet());
    DataflowNode targetNode = resolve(stateMap, step.target());
    if (targetNode==null) {
      if (require.isEmpty()) require.add(root);
    } else {
      require.add(targetNode);
    }
    var mainRefStepIndex = targetNode==null ? -2:targetNode.stepIndex;
    return new DataflowNode(stepIndex, step,  dmats,require, Sets.mutable.empty(), mainRefStepIndex);
  }



  static DataflowNode resolve(Map<String, DataflowNode> stateMap, Dataflows.DataMapping mapping) {
    if (mapping==null) return null;
    var ref = mapping.fromObj() != null? mapping.fromObj(): mapping.fromBody();
    if (ref == null) throw new DataflowParseException("Cannot resolve " + mapping, 500);
    return resolve(stateMap, ref);
  }

  static DataflowNode resolve(Map<String, DataflowNode> stateMap, String ref) {
    if (ref == null) return null;
    var target = extractTargetInFlow(ref);
    var node = stateMap.get(target);
    if (node==null) throw new DataflowParseException("Cannot resolve ref("+ref+")", 500);
    return node;
  }

  static String extractTargetInFlow(String exp) {
    String[] split = exp.split("\\.");
    return split[0];
  }

  public DataflowNode getRootNode() {
    return rootNode;
  }

  public Dataflows.Spec getMacroSpec() {
    return macroSpec;
  }

  public List<DataflowNode> getAllNode() {
    return stepNodes;
  }

  public DataflowNode getEndNode() {
    return endNode;
  }

  public record DataflowNode (
    int stepIndex,
    Dataflows.Step step,
    List<DataMapAndTransformer> dmats,
    Set<DataflowNode> require,
    Set<DataflowNode> next,
    int mainRefStepIndex){

    public DataflowNode(int stepIndex) {
      this(
        stepIndex,
        null,
        Lists.mutable.empty(),
        Sets.mutable.empty(),
        Sets.mutable.empty(),
        -2
      );
    }

    Set<DataflowNode> simplifyRequire() {
      if (require.isEmpty()) return Set.of();
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

    public boolean requireFanIn() {
      return require.size() > 1;
    }

    @Override
    public String toString() {
      return "DataflowNode{" +
        "stepIndex=" + stepIndex +
        ", step=" + step +
        ", dmats=[" + dmats.size() +
        "], require=[" + require.size() +
        "], next=[" + next.size() +
        "], mainRefStepIndex=" + mainRefStepIndex +
        '}';
    }
  }

  public record DataMapAndTransformer(DataflowNode node,
                               Dataflows.DataMapping mapping,
                               ODataTransformer transformer) {
    public DataMapAndTransformer(DataflowNode node, Dataflows.DataMapping mapping) {
      this(node, mapping, ODataTransformer.create(mapping));
    }
  }
}
