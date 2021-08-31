package org.hpcclab.msc.object;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.MscFunction;
import org.hpcclab.msc.object.repository.MscFuncRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class BuildInFunctionLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(BuildInFunctionLoader.class);

  @Inject
  ObjectMapper mapper;
  @Inject
  MscFuncRepository funcRepo;

  CompletionStage<Void> onStart(@Observes StartupEvent ev) throws URISyntaxException, IOException {
    ClassLoader classLoader = getClass().getClassLoader();

    URL resource = classLoader.getResource("/functions");

    // dun walk the root path, we will walk all the classes
    List<File> collect = Files.walk(Paths.get(resource.toURI()))
      .filter(Files::isRegularFile)
      .map(Path::toFile)
      .collect(Collectors.toList());

    var funcList = collect.stream()
      .map(file -> {
        MscFunction func = null;
        try {
          func = mapper.readValue(file, MscFunction.class);
        } catch (IOException e) {
          LOGGER.error("import build-in function fail {}", file.getName());
          throw new RuntimeException(e);
        }
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("import build-in function {}", func);
        } else {
          LOGGER.info("import build-in function {}", func.getName());
        }
        return func;
      })
      .collect(Collectors.toList());
    return funcRepo.persistOrUpdate(funcList)
      .subscribeAsCompletionStage();
  }
}
