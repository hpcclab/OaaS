package org.hpcclab.oaas.controller.rest;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidCreator;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.core.json.Json;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.hpcclab.oaas.controller.ClsManagerConfig;
import org.hpcclab.oaas.controller.service.OrbitStateManager;
import org.hpcclab.oaas.controller.service.PackageValidator;
import org.hpcclab.oaas.controller.service.ProvisionPublisher;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.cls.OClassDeploymentStatus;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.pkg.OPackage;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.OrbitManagerGrpc;
import org.hpcclab.oaas.proto.OrbitUpdateRequest;
import org.hpcclab.oaas.proto.OrbitUpdateRequestOrBuilder;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.ClassResolver;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.jboss.resteasy.reactive.RestQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
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
  ClsManagerConfig config;
  @Inject
  PackageValidator validator;
  @Inject
  ClassResolver classResolver;
  @Inject
  ProtoMapper protoMapper;
  @Inject
  OrbitStateManager orbitStateManager;
  @GrpcClient("orbit-manager")
  OrbitManagerGrpc.OrbitManagerBlockingStub orbitManager;

  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @JsonView(Views.Public.class)
  @RunOnVirtualThread
  public OPackage create(@RestQuery Boolean update,
                         @RestQuery @DefaultValue("false") Boolean overrideDeploy,
                         OPackage packageContainer) {
    var options = PackageValidator.ValidationOptions.builder()
      .overrideDeploymentStatus(overrideDeploy).build();
    var pkg = validator.validate(packageContainer, options).await().indefinitely();
    var classes = pkg.getClasses();
    var functions = pkg.getFunctions();
    var clsMap = classes.stream()
      .collect(Collectors.toMap(OClass::getKey, Function.identity()));
    var changedClasses = classResolver.resolveInheritance(clsMap);
    var partitioned = changedClasses.values()
      .stream()
      .collect(Collectors.partitioningBy(cls -> cls.getRev()==null));
    var newClasses = partitioned.get(true);
    var oldClasses = partitioned.get(false);
    refresh(oldClasses);
    refresh(newClasses);
    classRepo
      .atomic().persistWithRevAsync(oldClasses)
      .await().indefinitely();
    classRepo.persist(newClasses);
    funcRepo.persist(functions);
    for (var cls : changedClasses.values()) {
      classRepo.invalidate(cls.getKey());
    }
    for (var fn : functions) {
      funcRepo.invalidate(fn.getKey());
    }

    var pkgCls = changedClasses.values()
      .stream()
      .filter(cls -> Objects.equals(cls.getPkg(), pkg.getName()))
      .toList();
    pkg.setClasses(pkgCls);

    if (config.orbitEnabled()) {
      deploy(pkg);
    } else {
      for (OClass cls : pkg.getClasses()) {
        if (cls.getStatus()==null) {
          cls.setStatus(new OClassDeploymentStatus().setOrbitId(TsidCreator.getTsid1024().toLong()));
          classRepo.persist(cls);
        }
      }
    }
    if (config.kafkaEnabled()) {
      provisionPublisher.submitNewPkg(pkg).await().indefinitely();
    }
    if (logger.isDebugEnabled())
      logger.debug("pkg {}", Json.encodePrettily(pkg));

    return pkg;
  }

  void refresh(Collection<OClass> clsList) {
    var keys = clsList.stream().map(OClass::getKey).toList();
    classRepo.invalidate(keys);
    var clsMap = classRepo.list(keys);
    for (OClass cls : clsList) {
      var oldCls = clsMap.get(cls.getKey());
      if (oldCls==null)
        continue;
      cls.setStatus(oldCls.getStatus());
      logger.info("refresh cls {}", cls);
    }
  }

  @POST
  @Consumes("text/x-yaml")
  @RunOnVirtualThread
  public OPackage createByYaml(@RestQuery Boolean update,
                               @RestQuery @DefaultValue("false") Boolean overrideDeploy,
                               String body) {
    try {
      var pkg = yamlMapper.readValue(body, OPackage.class);
      return create(update, overrideDeploy, pkg);
    } catch (JsonProcessingException e) {
      throw new StdOaasException(e.getMessage(), 400);
    }
  }

  private void deploy(OPackage pkg) {
    for (var cls : pkg.getClasses()) {
      var resolvedFnList = cls.getResolved().getFunctions().values()
        .stream()
        .map(FunctionBinding::getFunction)
        .toList();
      var fnList = funcRepo.list(resolvedFnList);
      var protoFnList = fnList.values()
        .stream()
        .map(protoMapper::toProto)
        .toList();
      var unit = DeploymentUnit.newBuilder()
        .setCls(protoMapper.toProto(cls))
        .addAllFnList(protoFnList)
        .build();
      var orbitId = cls.getStatus() == null? 0 : cls.getStatus().getOrbitId();
      if (orbitId ==0) {
        logger.info("deploy a new orbit for cls [{}]", cls.getKey());
        var orbit = orbitManager.deploy(unit);
        orbitStateManager.updateOrbit(orbit).await().indefinitely();
        if (cls.getStatus()==null) cls.setStatus(new OClassDeploymentStatus());
        cls.getStatus().setOrbitId(orbit.getId());
        classRepo.persist(cls);
      } else {
        logger.info("update orbit [{}] for cls [{}]", orbitId, cls.getKey());
        var orbit = orbitStateManager.get(Tsid.from(orbitId).toLowerCase())
          .await().indefinitely();
        var req = OrbitUpdateRequest.newBuilder()
          .setOrbit(orbit)
          .setUnit(unit)
          .build();
        var newOrbitState = orbitManager.update(req);
        orbitStateManager.updateOrbit(newOrbitState);
      }

    }
  }
}
