package org.hpcclab.oaas.invoker.ispn;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invoker.ispn.repo.*;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.invocation.InvocationNode;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.ObjectRepoManager;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.lock.EmbeddedClusteredLockManagerFactory;
import org.infinispan.lock.api.ClusteredLockManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Startup
public class IspnProducer {
  public static final String OBJECT_CACHE = "OprcObject";
  public static final String INV_NODE_CACHE = "OprcInv";
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

//  @Produces
//  @ApplicationScoped
//  synchronized EmbeddedIspnObjectRepository objectRepository() {
//    Cache<String, OaasObject> cache;
//    if (!cacheManager.cacheExists(OBJECT_CACHE)) {
//      var conf = cacheCreator.createDistConfig(
//        confRegistry.getConfMap().get("OBJ"),
//        config.objStore(),
//        true,
//        OaasObject.class);
//      log(OBJECT_CACHE, conf);
//      cache = cacheManager.createCache(OBJECT_CACHE, conf);
//    } else {
//      cache = cacheManager.getCache(OBJECT_CACHE);
//    }
//    return new EmbeddedIspnObjectRepository(cache.getAdvancedCache());
//  }


  @Produces
  @ApplicationScoped
  ObjectRepoManager objectRepoManager(IspnCacheCreator cacheCreator,
                                      ClassRepository classRepository) {
    return new EmbededIspnObjectRepoManager(classRepository, cacheCreator);
  }

  @Produces
  @ApplicationScoped
  synchronized EmbeddedIspnClsRepository clsRepository() {
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
    return new EmbeddedIspnClsRepository(cache.getAdvancedCache());
  }

  @Produces
  @ApplicationScoped
  synchronized EmbeddedIspnFnRepository fnRepository() {
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
    return new EmbeddedIspnFnRepository(cache.getAdvancedCache());
  }

  @Produces
  @ApplicationScoped
  synchronized EmbeddedIspnInvNodeRepository invNodeRepository() {
    Cache<String, InvocationNode> cache;
    if (!cacheManager.cacheExists(INV_NODE_CACHE)) {
      var conf = cacheCreator.createDistConfig(
        confRegistry.getOrDefault("INV"),
        config.invStore(),
        false,
        InvocationNode.class);
      log(INV_NODE_CACHE, conf);
      cache = cacheManager.createCache(INV_NODE_CACHE, conf);
    } else {
      cache = cacheManager.getCache(INV_NODE_CACHE);
    }
    return new EmbeddedIspnInvNodeRepository(cache.getAdvancedCache());
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
  ClusteredLockManager clusteredLockManager(){
    return EmbeddedClusteredLockManagerFactory.from(cacheManager);
  }
}
