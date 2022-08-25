package org.hpcclab.oaas.controller.initializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.hpcclab.oaas.controller.rest.ModuleService;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
@RegisterForReflection(targets = ModuleService.Module.class)
public class BuiltInLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(BuiltInLoader.class);

  ObjectMapper mapper;
  @Inject
  ModuleService batchService;

  public void setup() throws ExecutionException, InterruptedException, IOException {
    mapper = new ObjectMapper(new YAMLFactory());

    List<OaasClass> classes = new ArrayList<>();
    List<OaasFunction> functions = new ArrayList<>();

    var files = List.of(
      "/builtin/builtin.logical.yml",
      "/builtin/builtin.basic.yml"
    );

    for (String file : files) {
      var is = getClass().getResourceAsStream(file);
      var batch = mapper.readValue(is, ModuleService.Module.class);
      var funcNames = batch.getFunctions().stream().map(OaasFunction::getName).toList();
      var clsNames = batch.getClasses().stream().map(OaasClass::getName).toList();
      LOGGER.info("from [{}] import functions {} and classes {}", file, funcNames, clsNames);
      classes.addAll(batch.getClasses());
      functions.addAll(batch.getFunctions());
    }

    ModuleService.Module batch = new ModuleService.Module()
      .setClasses(classes)
      .setFunctions(functions);

    batchService.create(batch)
      .subscribeAsCompletionStage().get();
  }
}
