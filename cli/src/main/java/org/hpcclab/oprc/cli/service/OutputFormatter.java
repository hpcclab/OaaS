package org.hpcclab.oprc.cli.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import org.hpcclab.oaas.model.OprcJsonUtil;
import org.hpcclab.oprc.cli.conf.OutputFormat;

import java.util.Collection;

import static org.hpcclab.oprc.cli.conf.OutputFormat.NDJSON;


@ApplicationScoped
public class OutputFormatter {
  ObjectMapper yamlMapper;
  ObjectMapper jsonMapper;
  ObjectMapper jsonPrettyMapper;


  @Inject
  public OutputFormatter(ObjectMapper mapper) {
    mapper.registerModule(OprcJsonUtil.createModule());
    this.jsonMapper = mapper;
    this.jsonPrettyMapper = mapper.enable(SerializationFeature.INDENT_OUTPUT);
    yamlMapper = mapper.copyWith(new YAMLFactory()
      .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
      .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
    );
    yamlMapper.registerModule(OprcJsonUtil.createModule());
//    yamlMapper.registerModule(new ProtobufModule());
  }

  @SneakyThrows
  public void print(OutputFormat format, JsonObject jsonObject) {
    if (jsonObject.containsKey("items")
      && jsonObject.containsKey("total")
      && format==NDJSON) {
      print(format, jsonObject.getJsonArray("items"));
      return;
    }
    switch (format) {
      case JSON, NDJSON -> System.out.println(jsonObject);
      case YAML -> System.out.println(yamlMapper.writeValueAsString(jsonObject.getMap()));
      case PJSON -> System.out.println(jsonObject.encodePrettily());
    }
  }

  @SneakyThrows
  public void printObject(OutputFormat format, Object val) {
    print(format, JsonObject.mapFrom(val));
  }

  @SneakyThrows
  public void printObject2(OutputFormat format, Object val) {
    switch (format) {
      case JSON, NDJSON -> System.out.println(jsonMapper.writeValueAsString(val));
      case YAML -> System.out.println(yamlMapper.writeValueAsString(val));
      case PJSON -> System.out.println(jsonPrettyMapper.writeValueAsString(val));
    }
  }


  @SneakyThrows
  public void printArray(OutputFormat format, Collection<Object> val) {
    print(format, JsonArray.of(val.toArray()));
  }

  @SneakyThrows
  public void print(OutputFormat format, JsonArray jsonArray) {
    switch (format) {
      case JSON -> System.out.println(jsonArray);
      case NDJSON -> {
        for (int i = 0; i < jsonArray.size(); i++) {
          System.out.println(Json.encode(jsonArray.getList().get(i)));
        }
      }
      case YAML -> System.out.println(yamlMapper.writeValueAsString(jsonArray.getList()));
      case PJSON -> System.out.println(jsonArray.encodePrettily());
    }
  }
}
