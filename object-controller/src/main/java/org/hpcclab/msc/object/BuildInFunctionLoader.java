package org.hpcclab.msc.object;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.StartupEvent;
import org.hpcclab.msc.object.entity.MscFunction;
import org.hpcclab.msc.object.repository.MscFuncRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

public class BuildInFunctionLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(BuildInFunctionLoader.class);

  @Inject
  ObjectMapper mapper;
  @Inject
  MscFuncRepository funcRepo;

  CompletionStage<Void> onStart(@Observes StartupEvent ev) throws URISyntaxException, IOException {
    var inputStream = getClass().getResourceAsStream("/functions/buildin.logical.json");

    var functions = mapper.readValue(inputStream,
      MscFunction[].class);
    var functionStream = Stream.of(functions)
      .peek(func -> {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("import build-in function {}", func);
        } else {
          LOGGER.info("import build-in function {}", func.getName());
        }
      });
    return funcRepo.persistOrUpdate(functionStream)
      .subscribeAsCompletionStage();
  }
}
