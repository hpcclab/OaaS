package org.hpcclab.msc.object;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.function.OaasFunction;
import org.hpcclab.msc.object.model.OaasClassDto;
import org.hpcclab.msc.object.model.OaasFunctionDto;
import org.hpcclab.msc.object.repository.OaasClassRepository;
import org.hpcclab.msc.object.repository.OaasFuncRepository;
import org.hpcclab.msc.object.resource.ClassResource;
import org.hpcclab.msc.object.resource.FunctionResource;
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
public class BuiltInLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(BuiltInLoader.class);

  ObjectMapper mapper;
  @Inject
  OaasFuncRepository funcRepository;
  @Inject
  OaasClassRepository classRepository;

  @Transactional
  @Blocking
  void onStart(@Observes StartupEvent startupEvent) throws ExecutionException, InterruptedException {
    mapper = new ObjectMapper(new YAMLFactory());

    loadFile(OaasFunctionDto.class,
      "/functions/builtin.logical.yml",
      "/functions/builtin.hls.yml"
    )
      .onItem().transformToUniAndConcatenate(func -> {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("import build-in function {}", func);
        } else {
          LOGGER.info("import build-in function {}", func.getName());
        }
        return funcRepository.save(func);
      })
      .collect().last()
//      .call(funcRepository::flush)
      .subscribeAsCompletionStage()
      .get();

    loadFile(OaasClassDto.class,
      "/classes/builtin.basic.yml"
    )
      .onItem().transformToUniAndConcatenate(classDto -> {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("import build-in class {}", classDto);
        } else {
          LOGGER.info("import build-in class {}", classDto.getName());
        }
        return classRepository.save(classDto);
      }).collect().last()
      .call(classRepository::flush)
      .subscribeAsCompletionStage()
      .get();;
  }

  <T> Multi<T> loadFile(Class<T> cls, String... files) {
    return Multi.createFrom().items(
      Stream.of(files)
        .flatMap(file -> {
          try {
            var is = getClass().getResourceAsStream(file);
            return Stream.of((T[]) mapper.readValue(is, cls.arrayType()));
          } catch (Throwable e) {
            throw new RuntimeException("get error on file " + file, e);
          }
        })
    );
  }
}
