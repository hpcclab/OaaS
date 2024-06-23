package org.hpcclab.oaas.pm.rest;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.core.json.Json;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.pkg.OPackage;
import org.hpcclab.oaas.pm.PkgManagerConfig;
import org.hpcclab.oaas.pm.service.CrStateManager;
import org.hpcclab.oaas.pm.service.PackagePublisher;
import org.hpcclab.oaas.proto.CrOperationResponse;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.OClassStatusUpdate;
import org.hpcclab.oaas.proto.OFunctionStatusUpdate;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.ClassResolver;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.hpcclab.oaas.repository.PackageValidator;
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
  PackagePublisher packagePublisher;
  @Inject
  PkgManagerConfig config;
  @Inject
  PackageValidator validator;
  @Inject
  ClassResolver classResolver;
  @Inject
  ProtoMapper protoMapper;
  @Inject
  CrStateManager crStateManager;

  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @JsonView(Views.Public.class)
  @RunOnVirtualThread
  public OPackage create(OPackage packageContainer) {
    var pkg = validator.validate(packageContainer);
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
    refreshFn(pkg.getFunctions());

    if (config.crmEnabled() && !pkg.getClasses().isEmpty()) {
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
      packagePublisher.submitNewPkg(pkg).await().indefinitely();
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
      logger.debug("refresh cls {}", cls.getKey());
    }
  }

  void refreshFn(Collection<OFunction> fnList) {
    var keys = fnList.stream().map(OFunction::getKey).toList();
    funcRepo.invalidate(keys);
    var fnMap = funcRepo.list(keys);
    for (var fn : fnList) {
      var oldFn = fnMap.get(fn.getKey());
      if (oldFn==null)
        continue;
      fn.setStatus(oldFn.getStatus());
      logger.debug("refresh fn {} {}", fn.getKey(), fn.getStatus());
    }
  }

  @POST
  @Consumes("text/x-yaml")
  @RunOnVirtualThread
  public OPackage createByYaml(String body) {
    try {
      var pkg = yamlMapper.readValue(body, OPackage.class);
      return create(pkg);
    } catch (JsonProcessingException e) {
      throw new StdOaasException(e.getMessage(), 400);
    }
  }

  private void deploy(OPackage pkg) {
    if (pkg.isDisable())
      return;
    for (var cls : pkg.getClasses()) {
      if (cls.isDisabled())
        continue;
      var resolvedFnList = cls.getResolved()
        .getFunctions().values()
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
        .toList();
      logger.info("deploy [cls={}, fnList={}]", cls.getKey(), resolvedFnList);
      var unit = DeploymentUnit.newBuilder()
        .setCls(protoMapper.toProto(cls))
        .addAllFnList(protoFnList)
        .build();
      var response = crStateManager.deploy(unit);
      updateState(pkg, response);
    }
  }

  void updateState(OPackage pkg, CrOperationResponse response) {
    for (OClassStatusUpdate statusUpdate : response.getClsUpdatesList()) {
      var classOptional = pkg.getClasses().stream()
        .filter(c -> c.getKey().equals(statusUpdate.getKey()))
        .findAny();
      if (classOptional.isEmpty()) continue;
      var cls = classOptional.get();
      cls.setStatus(protoMapper.fromProto(statusUpdate.getStatus()));
    }
    for (OFunctionStatusUpdate statusUpdate : response.getFnUpdatesList()) {
      var status = protoMapper.fromProto(statusUpdate.getStatus());
      var provision = protoMapper.fromProto(statusUpdate.getProvision());
      logger.debug("update func state {} {} {}",
        statusUpdate.getKey(), status, provision);
      var functionOptional = pkg.getFunctions().stream()
        .filter(f -> f.getKey().equals(statusUpdate.getKey()))
        .findAny();
      if (functionOptional.isEmpty()) continue;
      var fn = functionOptional.get();
      fn.setProvision(provision);
      fn.setStatus(status);
    }
  }
}
