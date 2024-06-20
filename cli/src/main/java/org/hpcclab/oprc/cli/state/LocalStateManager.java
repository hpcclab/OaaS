package org.hpcclab.oprc.cli.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.hpcclab.oaas.invocation.controller.*;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.repository.MapEntityRepository;
import org.hpcclab.oprc.cli.conf.ConfigFileManager;
import org.hpcclab.oprc.cli.conf.FileCliConfig;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Pawissanutt
 */
@ApplicationScoped
public class LocalStateManager {
  final FileCliConfig.LocalDevelopment localDev;
  final ObjectMapper objectMapper;
  final BaseClassControllerRegistry controllerRegistry;
  MapEntityRepository.MapClsRepository clsRepo;
  MapEntityRepository.MapFnRepository fnRepo;
  LocalObjRepoManager objRepoManager;
  StateManager stateManager;
  RepoCtxLoader ctxLoader;

  public LocalStateManager(ConfigFileManager fileManager,
                           ObjectMapper mapper) {
    this.localDev = fileManager.getDefault().getLocalDev();
    this.objectMapper = mapper;
    this.controllerRegistry = new BaseClassControllerRegistry();
  }

  void init() throws IOException {
    if (objRepoManager != null) return;
    StateModels.LocalPackage localPackage = loadLocal();
    MutableMap<String, OClass> clsMap = Maps.mutable.empty();
    localPackage.classes().forEach(v -> clsMap.put(v.getKey(), v));
    clsRepo = new MapEntityRepository.MapClsRepository(clsMap);
    MutableMap<String, OFunction> fnMap = Maps.mutable.empty();
    localPackage.functions().forEach(v -> fnMap.put(v.getKey(), v));
    fnRepo = new MapEntityRepository.MapFnRepository(fnMap);
    objRepoManager = new LocalObjRepoManager(controllerRegistry, localDev.localStatePath());
    stateManager = new RepoStateManager(objRepoManager);
    ctxLoader = new RepoCtxLoader(objRepoManager, controllerRegistry);
  }

  void buildClsController(ClassControllerBuilder builder) {
    for (OClass cls : clsRepo.getMap()) {
      ClassController con = builder.build(cls).await().indefinitely();
      controllerRegistry.register(con);
    }
  }

  StateModels.LocalPackage loadLocal() throws IOException {
    File pkgFile = localDev.localStatePath().resolve(localDev.localPackageFile()).toFile();
    if (!pkgFile.exists())
      return StateModels.LocalPackage.builder().functions(List.of()).classes(List.of()).build();
    return objectMapper.readValue(pkgFile, StateModels.LocalPackage.class);
  }
}
