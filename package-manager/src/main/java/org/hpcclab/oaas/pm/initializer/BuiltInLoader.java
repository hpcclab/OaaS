package org.hpcclab.oaas.pm.initializer;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.pkg.OPackage;
import org.hpcclab.oaas.pm.rest.PackageResource;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

@ApplicationScoped
public class BuiltInLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(BuiltInLoader.class);
  final PackageResource pkgService;
  final FunctionRepository fnRepo;

  public BuiltInLoader(PackageResource pkgService, FunctionRepository fnRepo) {
    this.pkgService = pkgService;
    this.fnRepo = fnRepo;
  }

  public void setup() throws IOException {
    var fn = fnRepo.get("builtin.logical.new");
    if (fn!=null) return;

    var mapper = new YAMLMapper();

    var files = List.of(
      "/builtin/builtin.logical.yml"
    );

    for (String file : files) {
      var is = getClass().getResourceAsStream(file);
      var pkg = mapper.readValue(is, OPackage.class);
      var funcList = pkg.getFunctions();
      var funcNames = funcList==null ? List.of()
        :funcList.stream()
        .map(f -> f.setPkg(pkg.getName()))
        .map(OFunction::getKey)
        .toList();
      var clsList = pkg.getClasses();
      var clsNames = clsList==null ? List.of()
        :clsList.stream()
        .map(c -> c.setPkg(pkg.getName()))
        .map(OClass::getKey)
        .toList();
      LOGGER.info("from [{}] import functions {} and classes {}", file, funcNames, clsNames);
      pkgService.create(pkg);
    }

  }
}
