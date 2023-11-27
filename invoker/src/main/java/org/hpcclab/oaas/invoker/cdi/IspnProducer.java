package org.hpcclab.oaas.invoker.cdi;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invoker.ispn.IspnCacheCreator;
import org.hpcclab.oaas.invoker.ispn.IspnConfig;
import org.hpcclab.oaas.invoker.ispn.lookup.LocationRegistry;
import org.hpcclab.oaas.invoker.ispn.lookup.LookupManager;
import org.hpcclab.oaas.invoker.ispn.repo.EIspnClsRepository;
import org.hpcclab.oaas.invoker.ispn.repo.EIspnFnRepository;
import org.hpcclab.oaas.invoker.ispn.repo.EIspnInvRepoManager;
import org.hpcclab.oaas.invoker.ispn.repo.EIspnObjectRepoManager;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.ObjectRepoManager;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.lock.EmbeddedClusteredLockManagerFactory;
import org.infinispan.lock.api.ClusteredLockManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.infinispan.commons.dataconversion.MediaType.APPLICATION_OBJECT_TYPE;
import static org.infinispan.commons.dataconversion.MediaType.TEXT_PLAIN_TYPE;

@ApplicationScoped
@Startup
public class IspnProducer {
  public static final String CLASS_CACHE = "OprcClass";
  public static final String FUNCTION_CACHE = "OprcFunction";
  private static final Logger logger = LoggerFactory.getLogger(IspnProducer.class);
  @Inject
  EmbeddedCacheManager cacheManager;
  @Inject
  IspnCacheCreator cacheCreator;
  @Inject
  IspnConfig config;
  DatastoreConfRegistry confRegistry = DatastoreConfRegistry.getDefault();

  @Produces
  @ApplicationScoped
  ObjectRepoManager objectRepoManager(IspnCacheCreator cacheCreator,
                                      ClassRepository classRepository) {
    return new EIspnObjectRepoManager(classRepository, cacheCreator);
  }

  @Produces
  @ApplicationScoped
  synchronized EIspnClsRepository clsRepository() {
    Cache<String, OaasClass> cache;
    if (!cacheManager.cacheExists(CLASS_CACHE)) {
      var conf = cacheCreator.createSimpleConfig(
        confRegistry.getOrDefault("PKG"),
        config.clsStore(),
        OaasClass.class);
      log(CLASS_CACHE, conf);
      cache = cacheManager.createCache(CLASS_CACHE, conf);
    } else {
      cache = cacheManager.getCache(CLASS_CACHE);
    }
    return new EIspnClsRepository(cache.getAdvancedCache());
  }

  @Produces
  @ApplicationScoped
  synchronized EIspnFnRepository fnRepository() {
    Cache<String, OaasFunction> cache;
    if (!cacheManager.cacheExists(FUNCTION_CACHE)) {
      var conf = cacheCreator.createSimpleConfig(
        confRegistry.getOrDefault("PKG"),
        config.fnStore(),
        OaasFunction.class);
      log(FUNCTION_CACHE, conf);
      cache = cacheManager.createCache(FUNCTION_CACHE, conf);
    } else {
      cache = cacheManager.getCache(FUNCTION_CACHE);
    }
    return new EIspnFnRepository(cache.getAdvancedCache());
  }

  @Produces
  @ApplicationScoped
  synchronized EIspnInvRepoManager invNodeRepository(IspnCacheCreator cacheCreator,
                                                     ClassRepository classRepository) {
    return new EIspnInvRepoManager(classRepository, cacheCreator);
  }

  private void log(String name, Configuration configuration) {
    if (logger.isDebugEnabled()) {
      logger.debug("create cache for {} : {}", name, configuration);
    } else {
      logger.info("create cache for {}", name);
    }
  }

  @Produces
  @ApplicationScoped
  LocationRegistry locationRegistry() {
    var name = "LocationRegistry";
    if (!cacheManager.cacheExists(name)) {
      var conf = new ConfigurationBuilder()
        .clustering().cacheMode(CacheMode.REPL_SYNC)
        .encoding()
        .key().mediaType(TEXT_PLAIN_TYPE)
        .encoding()
        .value().mediaType(APPLICATION_OBJECT_TYPE)
        .build();
      cacheManager.createCache(name, conf);
    }
    var locationRegistry= new LocationRegistry(cacheManager.getCache(name));
    locationRegistry.initLocal();
    return locationRegistry;
  }

  @Produces
  @ApplicationScoped
  LookupManager lookupManager(LocationRegistry locationRegistry) {
    return new LookupManager(locationRegistry);
  }

  @Produces
  @ApplicationScoped
  ClusteredLockManager clusteredLockManager() {
    return EmbeddedClusteredLockManagerFactory.from(cacheManager);
  }
}
