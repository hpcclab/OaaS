package org.hpcclab.oaas.invocation.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import org.hpcclab.oaas.model.function.Dataflows;
import org.hpcclab.oaas.model.object.JsonBytes;

import java.util.List;

/**
 * @author Pawissanutt
 */
public class JaywayDataTransformer implements ODataTransformer {

  ObjectMapper mapper;
  ParseContext parseContext;
  Configuration conf;
  List<Dataflows.Transformation> transformations;
  List<JsonPath> readPaths;

  public JaywayDataTransformer(List<Dataflows.Transformation> transformations) {
    if (transformations==null) transformations = List.of();
    mapper = new ObjectMapper();
    conf = Configuration
      .builder()
      .mappingProvider(new JacksonMappingProvider(mapper))
      .jsonProvider(new JacksonJsonNodeJsonProvider(mapper))
      .options(Option.SUPPRESS_EXCEPTIONS)
      .build();
    parseContext = JsonPath.using(conf);
    this.transformations = transformations;
    readPaths = transformations.stream()
      .map(t -> {
        if (t.path()==null || t.path().isEmpty())
          return null;
        return JsonPath.compile(t.path());
      })
      .toList();
  }

  @Override
  public JsonBytes transform(JsonBytes map) {
    if (transformations.isEmpty()) return map;
    if (map.getNode()==null) return JsonBytes.EMPTY;
    ObjectNode mappingNode = map.getNode();
    ObjectNode outputNode = mapper.createObjectNode();
    outputNode = map(outputNode, mappingNode);
    return new JsonBytes(outputNode);
  }

  @Override
  public JsonBytes transformMerge(JsonBytes mergeInto, JsonBytes map) {
    if (transformations.isEmpty()) return mergeInto;
    if (map.getNode()==null) return mergeInto;
    ObjectNode mappingNode = map.getNode();
    ObjectNode outputNode = mergeInto.getNode();
    outputNode = map(outputNode, mappingNode);
    return new JsonBytes(outputNode);
  }

  private ObjectNode map(ObjectNode base, ObjectNode map) {
    for (int i = 0; i < readPaths.size(); i++) {
      var tran = transformations.get(i);
      var inject = tran.inject()==null ? "":tran.inject();
      JsonPath path = readPaths.get(i);
      Object out;
      if (path==null) {
        base.set(tran.inject(), map);
        out = map;
      } else {
        out = path.read(map, conf);
      }
      if (inject.isEmpty() && out instanceof ObjectNode on) {
        base = on;
      } else if (out instanceof JsonNode jn) {
        base.set(tran.inject(), jn);
      } else {
        base.set(tran.inject(), mapper.valueToTree(out));
      }
    }
    return base;
  }
}
