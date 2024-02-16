package org.hpcclab.oprc.cli.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import org.hpcclab.oprc.cli.mixin.CommonOutputMixin;

import java.util.Collection;

import static org.hpcclab.oprc.cli.mixin.CommonOutputMixin.OutputFormat.NDJSON;


@ApplicationScoped
public class OutputFormatter {
  ObjectMapper yamlMapper;


  @Inject
  public OutputFormatter(ObjectMapper mapper) {
    yamlMapper = mapper.copyWith(new YAMLFactory()
      .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
      .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
    );
    yamlMapper.registerModule(new ProtobufModule());
  }

  @SneakyThrows
  public void print(CommonOutputMixin.OutputFormat format, JsonObject jsonObject) {
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
  public void printObject(CommonOutputMixin.OutputFormat format, Object val) {
    print(format, JsonObject.mapFrom(val));
  }
  @SneakyThrows
  public void printArray(CommonOutputMixin.OutputFormat format, Collection<Object> val) {
    print(format, JsonArray.of(val.toArray()));
  }

  @SneakyThrows
  public void print(CommonOutputMixin.OutputFormat format, JsonArray jsonArray) {
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
