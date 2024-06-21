package org.hpcclab.oprc.cli.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.MutableMap;
import org.hpcclab.oaas.invocation.BuiltInLoader;
import org.hpcclab.oaas.invocation.controller.*;
import org.hpcclab.oaas.invocation.controller.fn.CdiFunctionControllerFactory;
import org.hpcclab.oaas.invocation.metrics.MetricFactory;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.repository.MapEntityRepository;
import org.hpcclab.oaas.repository.id.TsidGenerator;
import org.hpcclab.oprc.cli.conf.ConfigFileManager;
import org.hpcclab.oprc.cli.conf.FileCliConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

/**
 * @author Pawissanutt
 */
@ApplicationScoped
public class LocalDevManager {
  private static final Logger logger = LoggerFactory.getLogger(LocalDevManager.class);
  final FileCliConfig.LocalDevelopment localDev;
  final ObjectMapper yamlMapper;
  final BaseClassControllerRegistry controllerRegistry;
  final CdiFunctionControllerFactory fnControllerFactory;
  MapEntityRepository.MapClsRepository clsRepo;
  MapEntityRepository.MapFnRepository fnRepo;
  LocalObjRepoManager objRepoManager;
  WrapStateManager stateManager;
  RepoCtxLoader ctxLoader;
  RepoClassControllerBuilder builder;

  public LocalDevManager(ConfigFileManager fileManager,
                         CdiFunctionControllerFactory fnControllerFactory) {
    this.localDev = fileManager.createDefault().getLocalDev();
    this.yamlMapper = new ObjectMapper(new YAMLFactory());
    this.controllerRegistry = new BaseClassControllerRegistry();
    this.fnControllerFactory = fnControllerFactory;
  }

  void init() {
    if (objRepoManager!=null) return;
    try {
      StateModels.LocalPackage localPackage = loadLocal();
      MutableMap<String, OClass> clsMap = Maps.mutable.empty();
      localPackage.classes().forEach(v -> clsMap.put(v.getKey(), v));
      clsRepo = new MapEntityRepository.MapClsRepository(clsMap);
      logger.debug("loaded cls {}", clsRepo.getMap().keySet());
      MutableMap<String, OFunction> fnMap = Maps.mutable.empty();
      localPackage.functions().forEach(v -> fnMap.put(v.getKey(), v));
      fnRepo = new MapEntityRepository.MapFnRepository(fnMap);
      logger.debug("loaded fn {}", fnRepo.getMap().keySet());
      objRepoManager = new LocalObjRepoManager(controllerRegistry, localDev.localStatePath());
      var internal = new RepoStateManager(objRepoManager);
      stateManager = new WrapStateManager(internal);
      ctxLoader = new RepoCtxLoader(objRepoManager, controllerRegistry);
      BuiltInLoader builtInLoader = new BuiltInLoader(clsRepo, fnRepo);
      builtInLoader.setup();
      buildClsController();
      for (OClass cls : clsRepo.getMap()) {
        ClassController con = builder.build(cls).await().indefinitely();
        controllerRegistry.register(con);
      }
    } catch (IOException e) {
      throw new StdOaasException("error on initializing state", e);
    }
  }

  void buildClsController() {
    builder = new RepoClassControllerBuilder(
      fnControllerFactory,
      stateManager,
      new TsidGenerator(),
      request -> Uni.createFrom().nullItem(),
      new MetricFactory.NoOpMetricFactory(),
      fnRepo,
      clsRepo
    );
  }

  StateModels.LocalPackage loadLocal() throws IOException {
    File pkgFile = localDev.localStatePath().resolve(localDev.localPackageFile()).toFile();
    if (!pkgFile.exists())
      return StateModels.LocalPackage.builder().functions(List.of()).classes(List.of()).build();
    return yamlMapper.readValue(pkgFile, StateModels.LocalPackage.class);
  }

  public void persistPkg() throws IOException {
    StateModels.LocalPackage localPackage = StateModels.LocalPackage.builder()
      .classes(clsRepo.getMap().values().stream().toList())
      .functions(fnRepo.getMap().values().stream().toList())
      .build();
    Files.createDirectories(localDev.localStatePath());
    File pkgFile = localDev.localStatePath().resolve(localDev.localPackageFile()).toFile();
    yamlMapper.writeValue(pkgFile, localPackage);
    logger.debug("update pkg {}}", pkgFile);
  }

  public void persistObject() throws IOException {
    Set<String> dirtyCls = stateManager.dirtyCls;
    for (String cls : dirtyCls) {
      logger.debug("persisting object in class '{}'", cls);
      objRepoManager.persist(cls);
    }
  }

  public MapEntityRepository.MapClsRepository getClsRepo() {
    init();
    return clsRepo;
  }

  public MapEntityRepository.MapFnRepository getFnRepo() {
    init();
    return fnRepo;
  }

  public RepoCtxLoader getCtxLoader() {
    init();
    return ctxLoader;
  }

  public BaseClassControllerRegistry getControllerRegistry() {
    init();
    return controllerRegistry;
  }

  public LocalObjRepoManager getObjRepoManager() {
    init();
    return objRepoManager;
  }

  public RepoClassControllerBuilder getClassControllerBuilder() {
    return builder;
  }


  static class WrapStateManager implements StateManager {

    final StateManager internal;
    Set<String> dirtyCls = Sets.mutable.empty();

    public WrapStateManager(StateManager internal) {
      this.internal = internal;
    }

    @Override
    public Uni<Void> applySimple(SimpleStateOperation operation) {
      if (operation.getUpdateCls()!=null) {
        dirtyCls.add(operation.getUpdateCls().getKey());
      }
      if (operation.getCreateCls()!=null) {
        dirtyCls.add(operation.getCreateCls().getKey());
      }
      return internal.applySimple(operation);
    }
  }
}
