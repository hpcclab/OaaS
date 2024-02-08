package org.hpcclab.oaas.controller.rest;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.f4b6a3.tsid.Tsid;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.core.json.Json;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.controller.PkgManagetConfig;
import org.hpcclab.oaas.controller.service.CrStateManager;
import org.hpcclab.oaas.controller.service.PackageValidator;
import org.hpcclab.oaas.controller.service.ProvisionPublisher;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.cls.OClassDeploymentStatus;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.pkg.OPackage;
import org.hpcclab.oaas.proto.*;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.ClassResolver;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.jboss.resteasy.reactive.RestQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
  PkgManagetConfig config;
  @Inject
  PackageValidator validator;
  @Inject
  ClassResolver classResolver;
  @Inject
  ProtoMapper protoMapper;
  @Inject
  CrStateManager crStateManager;
  @GrpcClient("orbit-manager")
  CrManagerGrpc.CrManagerBlockingStub crManager;

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
    var pkgCls = changedClasses.values()
      .stream()
      .filter(cls -> Objects.equals(cls.getPkg(), pkg.getName()))
      .toList();
    pkg.setClasses(pkgCls);


    refresh(pkg.getClasses());
    if (config.orbitEnabled()) {
      deploy(pkg);
    }

    var partitioned = changedClasses.values()
      .stream()
      .collect(Collectors.partitioningBy(cls -> cls.getRev()==null));
    var newClasses = partitioned.get(true);
    var oldClasses = partitioned.get(false);
    classRepo
      .atomic().persistWithRevAsync(oldClasses)
      .await().indefinitely();
    classRepo.persist(newClasses);
    funcRepo.persist(functions);

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
        .collect(Collectors.toSet());
      List<OFunction> fnList = Lists.mutable.empty();
      List<String> fnToLoad = Lists.mutable.empty();
      for (String key : resolvedFnList) {
        Optional<OFunction> fnOptional = pkg.getFunctions().stream()
          .filter(f -> f.getKey().equals(key))
          .findAny();
        if (fnOptional.isPresent()) fnList.add(fnOptional.get());
        else fnToLoad.add(key);
      }
      fnList.addAll(funcRepo.list(fnToLoad)
        .values());
      var protoFnList = fnList.stream()
        .map(protoMapper::toProto)
        .collect(Collectors.toList());
      logger.info("deploy [cls={}, fnList={}]", cls.getKey(), resolvedFnList);
      var unit = DeploymentUnit.newBuilder()
        .setCls(protoMapper.toProto(cls))
        .addAllFnList(protoFnList)
        .build();
      var orbitId = cls.getStatus()==null ? 0:cls.getStatus().getCrId();
      if (orbitId==0) {
        logger.info("deploy a new orbit for cls [{}]", cls.getKey());
        var response = crManager.deploy(unit);
        crStateManager.updateCr(response.getCr()).await().indefinitely();
        updateState(pkg, response);
      } else {
        logger.info("update orbit [{}] for cls [{}]", orbitId, cls.getKey());
        var orbit = crStateManager.get(Tsid.from(orbitId).toLowerCase())
          .await().indefinitely();
        var req = CrUpdateRequest.newBuilder()
          .setOrbit(orbit)
          .setUnit(unit)
          .build();
        var response = crManager.update(req);
        crStateManager.updateCr(response.getCr())
          .await().indefinitely();
        updateState(pkg, response);
      }
    }
  }

  void updateState(OPackage pkg, CrOperationResponse response) {
    var cr = response.getCr();
    for (OClassStatusUpdate statusUpdate : response.getClsUpdatesList()) {
      var classOptional = pkg.getClasses().stream()
        .filter(c -> c.getKey().equals(statusUpdate.getKey()))
        .findAny();
      if (classOptional.isEmpty()) continue;
      var cls = classOptional.get();
      cls.setStatus(protoMapper.fromProto(statusUpdate.getStatus()));
    }
    for (OFunctionStatusUpdate statusUpdate : response.getFnUpdatesList()) {
      var functionOptional = pkg.getFunctions().stream()
        .filter(f -> f.getKey().equals(statusUpdate.getKey()))
        .findAny();
      if (functionOptional.isEmpty()) continue;
      var fn = functionOptional.get();
      fn.setStatus(protoMapper.fromProto(statusUpdate.getStatus()));
    }
    for (var clsKey : cr.getAttachedClsList()) {
      var classOptional = pkg.getClasses().stream()
        .filter(c -> c.getKey().equals(clsKey))
        .findAny();
      if (classOptional.isEmpty()) continue;
      var cls = classOptional.get();
      if (cls.getStatus()==null) cls.setStatus(new OClassDeploymentStatus());
      cls.getStatus().setCrId(cr.getId());
    }
  }
}
