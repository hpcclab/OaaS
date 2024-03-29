package org.hpcclab.oaas.controller.rest;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.config.WithDefault;
import io.smallrye.mutiny.Uni;
import org.apache.commons.lang3.NotImplementedException;
import org.hpcclab.oaas.arango.ArgDataAccessException;
import org.hpcclab.oaas.controller.OcConfig;
import org.hpcclab.oaas.controller.service.ProvisionPublisher;
import org.hpcclab.oaas.controller.service.PackageValidator;
import org.hpcclab.oaas.model.pkg.OaasPackageContainer;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.ClassResolver;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.jboss.resteasy.reactive.RestQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/packages")
public class PackageResource {
  private static final Logger logger = LoggerFactory.getLogger(PackageResource.class);

  @Inject
  ClassRepository classRepo;
  @Inject
  FunctionRepository funcRepo;
  @Inject
  ProvisionPublisher provisionPublisher;
  @Inject
  OcConfig config;
  @Inject
  PackageValidator validator;
  @Inject
  ClassResolver classResolver;

  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @JsonView(Views.Public.class)
  public Uni<OaasPackageContainer> create(@RestQuery Boolean update,
                                          @RestQuery @DefaultValue("false") Boolean overrideDeploy,
                                          OaasPackageContainer packageContainer) {
    var options = PackageValidator.ValidationOptions.builder()
      .overrideDeploymentStatus(overrideDeploy).build();
    var uni = validator.validate(packageContainer, options)
      .flatMap(pkg -> {
        var classes = pkg.getClasses();
        var functions = pkg.getFunctions();
        var clsMap = classes.stream()
          .collect(Collectors.toMap(OaasClass::getKey, Function.identity()));
        var changedClasses = classResolver.resolveInheritance(clsMap);
        var partitioned = changedClasses.values()
          .stream()
          .collect(Collectors.partitioningBy(cls -> cls.getRev()==null));
        var newClasses = partitioned.get(true);
        var oldClasses = partitioned.get(false);
        return classRepo
          .atomic().persistWithPreconditionAsync(oldClasses)
          .flatMap(__ -> classRepo.persistAsync(newClasses))
          .flatMap(__ -> funcRepo.persistAsync(functions))
          .replaceWith(() -> {
            var pkgCls = changedClasses.values()
              .stream()
              .filter(cls -> Objects.equals(cls.getPkg(), pkg.getName()))
              .toList();
            return pkg.setClasses(pkgCls);
          });
      })
      .onFailure(ArgDataAccessException.class)
      .retry().atMost(3);
    if (config.kafkaEnabled()) {
      return uni.call(pkg ->
        provisionPublisher.submitNewPkg(pkg));
    }
    return uni;
  }

  @Path("{name}")
  @PATCH
  @Consumes(MediaType.APPLICATION_JSON)
  public Uni<OaasPackageContainer> patch(String name,
                                         OaasPackageContainer oaasPackage) {
    // TODO
    throw new NotImplementedException();
  }

  @Path("{name}")
  @PATCH
  @Consumes("text/x-yaml")
  public Uni<OaasPackageContainer> patchByYaml(String name,
                                               String body) {
    try {
      var pkg = yamlMapper.readValue(body, OaasPackageContainer.class);
      return patch(name, pkg);
    } catch (JsonProcessingException e) {
      throw new StdOaasException(e.getMessage(), 400);
    }
  }

  @POST
  @Consumes("text/x-yaml")
  public Uni<OaasPackageContainer> createByYaml(@RestQuery Boolean update,
                                                @RestQuery @DefaultValue("false") Boolean overrideDeploy,
                                                String body) {
    try {
      var pkg = yamlMapper.readValue(body, OaasPackageContainer.class);
      return create(update, overrideDeploy, pkg);
    } catch (JsonProcessingException e) {
      throw new StdOaasException(e.getMessage(), 400);
    }
  }
}
