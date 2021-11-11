package org.hpcclab.oaas;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.quarkus.runtime.StartupEvent;
import org.hpcclab.oaas.model.cls.OaasClassDto;
import org.hpcclab.oaas.model.function.OaasFunctionDto;
import org.hpcclab.oaas.service.BatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

@ApplicationScoped
public class BuiltInLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(BuiltInLoader.class);

  ObjectMapper mapper;
  @Inject
  BatchService batchService;

  @Transactional
  void onStart(@Observes StartupEvent startupEvent) throws ExecutionException, InterruptedException, IOException {
    mapper = new ObjectMapper(new YAMLFactory());

    List<OaasClassDto> classes = new ArrayList<>();
    List<OaasFunctionDto> functions = new ArrayList<>();

    var files = List.of(
      "/builtin/builtin.logical.yml",
      "/builtin/builtin.video.yml",
      "/builtin/builtin.basic.yml",
      "/builtin/builtin.text.yml"
    );

    for (String file : files) {
      var is = getClass().getResourceAsStream(file);
      var batch = mapper.readValue(is, BatchService.Batch.class);
      var funcNames = batch.getFunctions().stream().map(OaasFunctionDto::getName).toList();
      var clsNames = batch.getClasses().stream().map(OaasClassDto::getName).toList();
      LOGGER.info("from [{}] import functions {} and classes {}", file, funcNames, clsNames);
      classes.addAll(batch.getClasses());
      functions.addAll(batch.getFunctions());
    }

    BatchService.Batch batch = new BatchService.Batch()
      .setClasses(classes)
      .setFunctions(functions);

    batchService.create(batch)
      .subscribeAsCompletionStage().get();
  }


  <T> Stream<T> loadFile(Class<T> cls, String... files) {
    return
      Stream.of(files)
        .map(file -> {
          try {
            var is = getClass().getResourceAsStream(file);
            return mapper.readValue(is, cls);
          } catch (IOException e) {
            throw new RuntimeException("get error on file " + file, e);
          }
        });
  }
}
