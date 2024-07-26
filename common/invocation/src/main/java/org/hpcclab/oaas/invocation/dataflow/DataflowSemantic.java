package org.hpcclab.oaas.invocation.dataflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Objects;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.MutableMap;
import org.hpcclab.oaas.invocation.transform.ODataTransformer;
import org.hpcclab.oaas.model.function.Dataflows;

import java.util.HashSet;
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
  List<DataflowNode> allNodes;
  DataflowNode endNode;


  protected DataflowSemantic(Dataflows.Spec macroSpec,
                             DataflowNode rootNode) {
    this.macroSpec = macroSpec;
    this.rootNode = rootNode;
    this.allNodes = Lists.mutable.empty();
  }


  public static DataflowSemantic construct(Dataflows.Spec spec) {
    spec = spec.cleanNull();
    var root = new DataflowNode(0);
    var df = new DataflowSemantic(spec, root);
    df.allNodes.add(root);
    TemplateProcessor templateProcessor = new JaywayTemplateProcessor();
    var steps = spec.steps();
    MutableMap<String, DataflowNode> stateMap = Maps.mutable.of("@", root);
    for (int i = 0; i < steps.size(); i++) {
      var step = steps.get(i);
      var node = createNode(i + 1, step, stateMap, df, templateProcessor);
      df.allNodes.add(node);
      if (step.as()!=null && !step.as().isEmpty()) {
        stateMap.put(step.as(), node);
      }
    }
    List<DataflowNode> nodes = df.allNodes;
    for (int i = 1; i < nodes.size(); i++) {
      DataflowNode node = nodes.get(i);
      node.simplifyRequire();
      node.markNext(null);
      if (node.require.isEmpty()) {
        root.next.add(node);
      }
    }
    var endStep = Dataflows.Step.builder()
      .target(spec.output())
      .bodyTemplate(spec.bodyTemplate())
      .build();
    df.endNode = createNode(steps.size(), endStep, stateMap, df, templateProcessor);
    df.allNodes.add(df.endNode);
    return df;
  }

  static DataflowNode createNode(int stepIndex,
                                 Dataflows.Step step,
                                 MutableMap<String, DataflowNode> stateMap,
                                 DataflowSemantic semantic,
                                 TemplateProcessor templateProcessor) {
    var template = step.bodyTemplate();
    TemplateProcessor.Replacer<JsonNode> bodyReplacer = template == null?
      null:
      templateProcessor.createReplacer(stateMap, template.getJsonNode());
    var argsTemplate  = step.args();
    TemplateProcessor.Replacer<Map<String, String>> argsReplacer = argsTemplate == null?
      null:
      templateProcessor.createMapReplacer(stateMap, argsTemplate);
    Set<Integer> requireIndexes = new HashSet<>();
    if (argsReplacer != null) requireIndexes.addAll(argsReplacer.getRequires());
    if (bodyReplacer != null) requireIndexes.addAll(bodyReplacer.getRequires());
    Set<DataflowNode> require = requireIndexes.stream()
      .map(i -> semantic.allNodes.get(i))
      .collect(Collectors.toSet());
    DataflowNode targetNode = resolve(stateMap, step.target());
    if (targetNode==null) {
      if (require.isEmpty()) require.add(semantic.rootNode);
    } else {
      require.add(targetNode);
    }
    var mainRefStepIndex = targetNode==null ? -1:targetNode.stepIndex;
    return new DataflowNode(
      stepIndex,
      step,
      require,
      Sets.mutable.empty(),
      bodyReplacer,
      argsReplacer,
      mainRefStepIndex);
  }


  static DataflowNode resolve(Map<String, DataflowNode> stateMap, Dataflows.DataMapping mapping) {
    if (mapping==null) return null;
    var ref = mapping.refName();
    if (ref==null || ref.isEmpty()) throw new DataflowParseException("Cannot resolve " + mapping, 500);
    return resolve(stateMap, ref);
  }

  static DataflowNode resolve(Map<String, DataflowNode> stateMap, String ref) {
    if (ref==null || ref.isEmpty()) return null;
    var target = extractTargetInFlow(ref);
    var node = stateMap.get(target);
    if (node==null) throw new DataflowParseException("Cannot resolve ref(" + ref + ") from "+ stateMap.keySet(), 500);
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
    return allNodes;
  }

  public DataflowNode getEndNode() {
    return endNode;
  }

  public record DataflowNode(
    int stepIndex,
    Dataflows.Step step,
    Set<DataflowNode> require,
    Set<DataflowNode> next,
    TemplateProcessor.Replacer<JsonNode> bodyReplacer,
    TemplateProcessor.Replacer<Map<String,String>> argsReplacer,
    int mainRefStepIndex) {

    public DataflowNode(int stepIndex) {
      this(
        stepIndex,
        null,
        Sets.mutable.empty(),
        Sets.mutable.empty(),
        null,
        null,
        -1
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

    @Override
    public String toString() {
      return "DataflowNode{" +
        "stepIndex=" + stepIndex +
        ", step=" + step +
        ", require=" + require.size() +
        ", next=" + next.size() +
        ", bodyReplacer=" + bodyReplacer +
        ", argsReplacer=" + argsReplacer +
        ", mainRefStepIndex=" + mainRefStepIndex +
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
