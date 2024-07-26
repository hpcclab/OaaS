package org.hpcclab.oaas.invocation.dataflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.invocation.InvocationCtx;

import java.util.*;

/**
 * @author Pawissanutt
 */
public class JaywayTemplateProcessor implements TemplateProcessor {

  ObjectMapper mapper;
  Configuration conf;

  public JaywayTemplateProcessor() {
    this(new ObjectMapper());
  }

  public JaywayTemplateProcessor(ObjectMapper mapper) {
    this.mapper = mapper;
    conf = Configuration
      .builder()
      .mappingProvider(new JacksonMappingProvider(mapper))
      .jsonProvider(new JacksonJsonNodeJsonProvider(mapper))
      .options(Option.SUPPRESS_EXCEPTIONS)
      .build();
  }

  boolean containsSubstitution(String text) {
    text = text.trim();
    if (!text.startsWith("${"))
      return false;
    return text.endsWith("}");
  }

  boolean containsSubstitution(Map<String, String> map) {
    for (var val : map.values()) {
      boolean b = containsSubstitution(val);
      if (b)
        return true;
    }
    return false;
  }

  boolean containsSubstitution(JsonNode node) {
    if (node.isTextual()) return containsSubstitution(node.asText());
    if (node.isArray() || node.isObject()) {
      for (JsonNode jsonNode : node) {
        boolean b = containsSubstitution(jsonNode);
        if (b)
          return true;
      }
    }
    return false;
  }

  @Override
  public Replacer<JsonNode> createReplacer(Map<String, DataflowSemantic.DataflowNode> stateMap,
                                           JsonNode template) {
    if (containsSubstitution(template)) {
      if (template.isTextual()) {
        return createReplacer(stateMap, template.asText());
      } else if (template.isArray()) {
        List<Replacer<JsonNode>> replacerList = new ArrayList<>();
        for (JsonNode jsonNode : template) {
          replacerList.add(createReplacer(stateMap, jsonNode));
        }
        return new ArrayReplacer(replacerList);
      } else if (template instanceof ObjectNode objectNode) {
        Map<String, Replacer<JsonNode>> replacerMap = new HashMap<>();
        for (var it = objectNode.fields(); it.hasNext(); ) {
          var entry = it.next();
          replacerMap.put(entry.getKey(), createReplacer(stateMap, entry.getValue()));
        }
        return new TreeReplacer(replacerMap);
      }
    }
    return new NoOpReplace(template);
  }

  @Override
  public Replacer<Map<String, String>> createMapReplacer(Map<String, DataflowSemantic.DataflowNode> stateMap,
                                                         Map<String, String> template) {
    if (!containsSubstitution(template))
      return new NoOpMapReplacer(template);
    Map<String, Replacer<JsonNode>> replacerMap = new HashMap<>();
    for (var entry : template.entrySet()) {
      String value = entry.getValue();
      if (containsSubstitution(value)) {
        replacerMap.put(entry.getKey(), createReplacer(stateMap, value));
      } else {
        replacerMap.put(entry.getKey(), new NoOpReplace(TextNode.valueOf(value)));
      }
    }
    return new MapReplacer(replacerMap);
  }

  Replacer<JsonNode> createReplacer(Map<String, DataflowSemantic.DataflowNode> stateMap,
                                    String substitution) {
    String trim = substitution.trim();
    String content = trim.substring(2, trim.length() - 1).trim();
    String[] src = content.split("\\|");
    if (src.length < 2) throw new DataflowParseException("cannot parse data substitution '" + substitution + "'");
    var target = src[0];
    var subTarget = src[1];
    var jsonPath = src.length==3 ? src[2]:null;
    if (subTarget.equalsIgnoreCase("args")) {
      return new ArgReplacer(jsonPath);
    } else if (subTarget.equalsIgnoreCase("body")) {
      DataflowSemantic.DataflowNode node = resolve(stateMap, target);
      return new BodyReplacer(node.stepIndex(),
        jsonPath==null ? null:JsonPath.compile(jsonPath));
    } else if (subTarget.equalsIgnoreCase("output")) {
      DataflowSemantic.DataflowNode node = resolve(stateMap, target);
      return new ObjReplacer(node.stepIndex(),
        jsonPath==null ? null:JsonPath.compile(jsonPath));
    } else {
      throw new DataflowParseException("unknown subtarget '" + subTarget + "' from "
        + "'" + substitution + "' with map"
        + stateMap.keySet());
    }
  }

  DataflowSemantic.DataflowNode resolve(Map<String, DataflowSemantic.DataflowNode> stateMap, String ref) {
    if (ref==null || ref.isEmpty()) return null;
    var node = stateMap.get(ref);
    if (node==null) throw new DataflowParseException("Cannot resolve ref(" + ref + ")", 500);
    return node;
  }


  static class ArgReplacer implements Replacer<JsonNode> {
    String argKey;

    public ArgReplacer(String argKey) {
      this.argKey = argKey;
    }

    @Override
    public JsonNode getReplaceVal(InvocationCtx ctx, DataflowState state) {
      Map<String, String> args = ctx.getRequest().args();
      if (args==null) {
        return NullNode.getInstance();
      } else {
        String v = args.get(argKey);
        return v==null ? NullNode.getInstance():TextNode.valueOf(v);
      }
    }

    @Override
    public Set<Integer> getRequires() {
      return Set.of(0);
    }
  }

  static class NoOpReplace implements Replacer<JsonNode> {
    final JsonNode node;

    public NoOpReplace(JsonNode node) {
      this.node = node;
    }

    @Override
    public JsonNode getReplaceVal(InvocationCtx ctx, DataflowState state) {
      return node.deepCopy();
    }
  }

  static class MapReplacer implements Replacer<Map<String, String>> {
    final Map<String, Replacer<JsonNode>> replacerMap;

    public MapReplacer(Map<String, Replacer<JsonNode>> replacerMap) {
      this.replacerMap = replacerMap;
    }

    @Override
    public Map<String, String> getReplaceVal(InvocationCtx ctx, DataflowState state) {
      Map<String, String> map = Maps.mutable.empty();
      for (var entry : replacerMap.entrySet()) {
        JsonNode node = entry.getValue().getReplaceVal(ctx, state);
        if (node.isValueNode())
          map.put(entry.getKey(), node.asText());
        else
          map.put(entry.getKey(), node.toString());
      }
      return map;
    }

    @Override
    public Set<Integer> getRequires() {
      Set<Integer> requires = new HashSet<>();
      for (var entry : replacerMap.entrySet()) {
        requires.addAll(entry.getValue().getRequires());
      }
      return requires;
    }
  }

  static class NoOpMapReplacer implements Replacer<Map<String, String>> {
    final Map<String, String> map;

    public NoOpMapReplacer(Map<String, String> map) {
      this.map = Collections.unmodifiableMap(map);
    }

    @Override
    public Map<String, String> getReplaceVal(InvocationCtx ctx, DataflowState state) {
      return map;
    }
  }

  abstract class JaywayReplacer implements Replacer<JsonNode> {
    final int srcIndex;
    final JsonPath path;

    JaywayReplacer(int srcIndex, JsonPath path) {
      this.srcIndex = srcIndex;
      this.path = path;
    }

    abstract JsonNode getBase(InvocationCtx ctx, DataflowState state);

    @Override
    public JsonNode getReplaceVal(InvocationCtx ctx, DataflowState state) {
      var base = getBase(ctx, state);
      if (path==null) return base;
      Object out = path.read(base, conf);
      if (out instanceof JsonNode jn) {
        return jn;
      } else {
        return mapper.valueToTree(out);
      }
    }

    @Override
    public Set<Integer> getRequires() {
      return Set.of(srcIndex);
    }
  }

  class BodyReplacer extends JaywayReplacer {

    BodyReplacer(int srcIndex, JsonPath path) {
      super(srcIndex, path);
    }

    @Override
    JsonNode getBase(InvocationCtx ctx, DataflowState state) {
      DataflowState.StepState stepState = state.stepStates()[srcIndex];
      ObjectNode node = stepState.body().getNode();
      return node==null ? NullNode.getInstance():node;
    }
  }

  class ObjReplacer extends JaywayReplacer {
    ObjReplacer(int srcIndex, JsonPath path) {
      super(srcIndex, path);
    }

    @Override
    JsonNode getBase(InvocationCtx ctx, DataflowState state) {
      DataflowState.StepState stepState = state.stepStates()[srcIndex];
      ObjectNode node = stepState.obj().getData().getNode();
      return node==null ? NullNode.getInstance():node;
    }
  }

  class TreeReplacer implements Replacer<JsonNode> {
    final Map<String, Replacer<JsonNode>> replacerMap;

    TreeReplacer(Map<String, Replacer<JsonNode>> replacerMap) {
      this.replacerMap = replacerMap;
    }

    @Override
    public JsonNode getReplaceVal(InvocationCtx ctx, DataflowState state) {
      ObjectNode node = mapper.createObjectNode();
      for (var entry : replacerMap.entrySet()) {
        node.set(entry.getKey(), entry.getValue().getReplaceVal(ctx, state));
      }
      return node;
    }

    @Override
    public Set<Integer> getRequires() {
      Set<Integer> requires = new HashSet<>();
      for (var entry : replacerMap.entrySet()) {
        requires.addAll(entry.getValue().getRequires());
      }
      return requires;
    }
  }

  class ArrayReplacer implements Replacer<JsonNode> {
    final List<Replacer<JsonNode>> replacerList;

    ArrayReplacer(List<Replacer<JsonNode>> replacerList) {
      this.replacerList = replacerList;
    }


    @Override
    public JsonNode getReplaceVal(InvocationCtx ctx, DataflowState state) {
      ArrayNode node = mapper.createArrayNode();
      for (var replacer : replacerList) {
        node.add(replacer.getReplaceVal(ctx, state));
      }
      return node;
    }


    @Override
    public Set<Integer> getRequires() {
      Set<Integer> requires = new HashSet<>();
      for (var replacer : replacerList) {
        requires.addAll(replacer.getRequires());
      }
      return requires;
    }

  }

}
