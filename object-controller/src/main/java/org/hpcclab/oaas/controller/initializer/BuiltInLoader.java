package org.hpcclab.oaas.controller.initializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.hpcclab.oaas.controller.rest.PackageService;
import org.hpcclab.oaas.model.OaasPackageContainer;
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
public class BuiltInLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(BuiltInLoader.class);

  ObjectMapper mapper;
  @Inject
  PackageService pkgService;

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
      var pkg = mapper.readValue(is, OaasPackageContainer.class);
      var funcList = pkg.getFunctions();
      var funcNames = funcList==null ? List.of()
        :funcList.stream()
        .map(f -> f.setPackageName(pkg.getName()))
        .map(OaasFunction::getKey)
        .toList();
      var clsList = pkg.getClasses();
      var clsNames = clsList==null ? List.of()
        :clsList.stream()
        .map(c -> c.setPackageName(pkg.getName()))
        .map(OaasClass::getKey)
        .toList();
      LOGGER.info("from [{}] import functions {} and classes {}", file, funcNames, clsNames);
      pkgService.create(true, pkg)
        .await().indefinitely();
    }

  }
}
