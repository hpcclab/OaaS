package org.hpcclab.oaas.controller.rest;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.arango.DataAccessException;
import org.hpcclab.oaas.controller.OcConfig;
import org.hpcclab.oaas.controller.service.FunctionProvisionPublisher;
import org.hpcclab.oaas.model.OaasPackageContainer;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class PackageResource implements PackageService {
  private static final Logger LOGGER = LoggerFactory.getLogger(PackageResource.class);

  @Inject
  ClassRepository classRepo;
  @Inject
  FunctionRepository funcRepo;
  @Inject
  FunctionProvisionPublisher provisionPublisher;
  @Inject
  OcConfig config;

  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  @Override
  @JsonView(Views.Public.class)
  public Uni<OaasPackageContainer> create(Boolean update,
                                          OaasPackageContainer pkg) {
    var classes = pkg.getClasses();
    var functions = pkg.getFunctions();
    for (OaasClass cls : classes) {
      cls.setPackageName(pkg.getName());
      cls.validate();
    }
    for (OaasFunction function : functions) {
      function.setPackageName(pkg.getName());
      function.validate();
    }

    var uni = Uni.createFrom().deferred(() -> {
        var clsMap = classes.stream()
          .collect(Collectors.toMap(OaasClass::getKey, Function.identity()));
        var changedClasses = classRepo.resolveInheritance(clsMap);
        var partitioned = changedClasses.values()
          .stream()
          .collect(Collectors.partitioningBy(cls -> cls.getRev()==null));
        var newClasses = partitioned.get(true);
        var oldClasses = partitioned.get(false);
        return classRepo
          .persistWithPreconditionAsync(oldClasses)
          .flatMap(__ -> classRepo.persistAsync(newClasses))
          .flatMap(__ -> funcRepo.persistAsync(functions));
      })
      .onFailure(DataAccessException.class)
      .retry().atMost(3);
    if (config.kafkaEnabled()) {
      return uni.call(__ ->
          provisionPublisher.submitNewFunction(pkg.getFunctions().stream()))
        .replaceWith(pkg);
    } else {
      return uni.replaceWith(pkg);
    }
  }

  @Override
  public Uni<OaasPackageContainer> patch(String name, OaasPackageContainer oaasPackage) {
    return null;
  }

  @Override
  public Uni<OaasPackageContainer> patchByYaml(String name, String body) {
    try {
      var pkg = yamlMapper.readValue(body, OaasPackageContainer.class);
      return patch(name, pkg);
    } catch (JsonProcessingException e) {
      throw new StdOaasException(e.getMessage(), 400);
    }
  }

  @Override
  @JsonView(Views.Public.class)
  public Uni<OaasPackageContainer> createByYaml(Boolean update,
                                                String body) {
    try {
      var pkg = yamlMapper.readValue(body, OaasPackageContainer.class);
      return create(update, pkg);
    } catch (JsonProcessingException e) {
      throw new StdOaasException(e.getMessage(), 400);
    }
  }
}
