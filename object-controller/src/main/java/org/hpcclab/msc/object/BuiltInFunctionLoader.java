package org.hpcclab.msc.object;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.function.OaasFunction;
import org.hpcclab.msc.object.repository.OaasFuncRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class BuiltInFunctionLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(BuiltInFunctionLoader.class);

  ObjectMapper mapper;
  @Inject
  OaasFuncRepository funcRepo;

  @ReactiveTransactional
  void onStart(@Observes StartupEvent startupEvent) throws ExecutionException, InterruptedException {
    mapper = new ObjectMapper(new YAMLFactory());

    var functions = Stream.of(
        "/functions/builtin.logical.yml",
        "/functions/builtin.hls.yml"
      )
      .flatMap(file -> {
        try {
          var is = getClass().getResourceAsStream(file);
          return Stream.of(mapper.readValue(is, OaasFunction[].class));
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
    funcRepo.persist(functions)
      .subscribeAsCompletionStage()
      .get();
  }
}
