package org.hpcclab.msc.object;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.quarkus.runtime.StartupEvent;
import org.hpcclab.msc.object.entity.function.MscFunction;
import org.hpcclab.msc.object.repository.MscFuncRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BuiltInFunctionLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(BuiltInFunctionLoader.class);

  ObjectMapper mapper;
  @Inject
  MscFuncRepository funcRepo;

  void onStart(@Observes StartupEvent ev) {
    mapper = new ObjectMapper(new YAMLFactory());

    var functions = Stream.of(
        "/functions/builtin.logical.yml",
        "/functions/builtin.hls.yml"
      )
      .flatMap(file -> {
        try {
          var is = getClass().getResourceAsStream(file);
          return Stream.of(mapper.readValue(is, MscFunction[].class));
        } catch (Throwable e) {
          throw new RuntimeException("get error on file " + file, e);
        }
      })
      .peek(func -> {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("import build-in function {}", func);
        } else {
          LOGGER.info("import build-in function {}", func.getName());
        }
      })
      .collect(Collectors.toList());
    funcRepo.persistOrUpdate(functions)
      .await().indefinitely();
  }
}
