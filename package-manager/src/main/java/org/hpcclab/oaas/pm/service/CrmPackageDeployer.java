package org.hpcclab.oaas.pm.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.pkg.OPackage;
import org.hpcclab.oaas.pm.PkgManagerConfig;
import org.hpcclab.oaas.proto.CrOperationResponse;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.OClassStatusUpdate;
import org.hpcclab.oaas.proto.OFunctionStatusUpdate;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.hpcclab.oaas.repository.PackageDeployer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Pawissanutt
 */
@ApplicationScoped
public class CrmPackageDeployer implements PackageDeployer {
  private static final Logger logger = LoggerFactory.getLogger( CrmPackageDeployer.class );
  final ProtoMapper protoMapper;
  final FunctionRepository funcRepo;
  final CrStateManager crStateManager;
  final PackagePublisher packagePublisher;
  final boolean crmEnabled;

  public CrmPackageDeployer(ProtoMapper protoMapper,
                            FunctionRepository funcRepo,
                            CrStateManager crStateManager,
                            PackagePublisher packagePublisher,
                            PkgManagerConfig config) {
    this.protoMapper = protoMapper;
    this.funcRepo = funcRepo;
    this.crStateManager = crStateManager;
    this.packagePublisher = packagePublisher;
    this.crmEnabled = config.crmEnabled();
  }

  @Override
  public void deploy(OPackage pkg) {
    if (pkg.isDisabled()
      || !crmEnabled
      || pkg.getClasses().isEmpty())
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
    packagePublisher.submitNewPkg(pkg).await().indefinitely();
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

  @Override
  public void detach(OClass cls) {
    crStateManager.detach(cls);
  }

  @Override
  public void detach(OFunction fn) {
    if (fn == null) return;
    packagePublisher.submitDeleteFn(fn.getKey());
  }
}
