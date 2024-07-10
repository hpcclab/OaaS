package org.hpcclab.oaas.invocation;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.pkg.OPackage;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;


public class BuiltInLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(BuiltInLoader.class);
  final ClassRepository clsRepo;
  final FunctionRepository fnRepo;

  public BuiltInLoader(ClassRepository clsRepo, FunctionRepository fnRepo) {
    this.clsRepo = clsRepo;
    this.fnRepo = fnRepo;
  }


  public void setup() throws IOException {
    var fn = fnRepo.get("builtin.new");
    if (fn!=null) return;

    var mapper = new YAMLMapper();

    var files = List.of(
      "/builtin/builtin.yml"
    );

    for (String file : files) {
      var is = getClass().getResourceAsStream(file);
      var pkg = mapper.readValue(is, OPackage.class);
      var funcList = pkg.getFunctions();
      var clsList = pkg.getClasses();
      for (var f : funcList) {
        f.setPkg(pkg.getName());
      }
      for (var c : clsList) {
        c.setPkg(pkg.getName());
      }
      if (LOGGER.isInfoEnabled()) {
        var funcNames = funcList.stream()
          .map(f -> f.setPkg(pkg.getName()))
          .map(OFunction::getKey)
          .toList();
        var clsNames = clsList.stream()
          .map(c -> c.setPkg(pkg.getName()))
          .map(OClass::getKey)
          .toList();
        LOGGER.info("from [{}] import functions {} and classes {}", file, funcNames, clsNames);
      }
      clsRepo.persist(clsList);
      fnRepo.persist(funcList);
    }

  }
}
