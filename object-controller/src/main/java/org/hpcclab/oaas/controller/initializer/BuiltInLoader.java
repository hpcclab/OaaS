package org.hpcclab.oaas.controller.initializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.hpcclab.oaas.controller.rest.PackageResource;
import org.hpcclab.oaas.model.pkg.OaasPackageContainer;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;

@ApplicationScoped
public class BuiltInLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(BuiltInLoader.class);

  ObjectMapper mapper;
  @Inject
  PackageResource pkgService;

  public void setup() throws IOException {
    mapper = new ObjectMapper(new YAMLFactory());

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
        .map(f -> f.setPkg(pkg.getName()))
        .map(OaasFunction::getKey)
        .toList();
      var clsList = pkg.getClasses();
      var clsNames = clsList==null ? List.of()
        :clsList.stream()
        .map(c -> c.setPkg(pkg.getName()))
        .map(OaasClass::getKey)
        .toList();
      LOGGER.info("from [{}] import functions {} and classes {}", file, funcNames, clsNames);
      pkgService.create(true, false, pkg);
    }

  }
}
