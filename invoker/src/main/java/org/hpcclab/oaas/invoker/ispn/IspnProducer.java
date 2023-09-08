package org.hpcclab.oaas.invoker.ispn;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invoker.ispn.repo.EmbeddedIspnClsRepository;
import org.hpcclab.oaas.invoker.ispn.repo.EmbeddedIspnFnRepository;
import org.hpcclab.oaas.invoker.ispn.repo.EmbeddedIspnInvNodeRepository;
import org.hpcclab.oaas.invoker.ispn.repo.EmbeddedIspnObjectRepository;
import org.hpcclab.oaas.invoker.ispn.store.ArgCacheStoreConfig;
import org.hpcclab.oaas.invoker.ispn.store.ArgConnectionConfig;
import org.hpcclab.oaas.invoker.ispn.store.ArgConnectionFactory;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.invocation.InvocationNode;
import org.hpcclab.oaas.model.object.OaasObject;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.util.concurrent.IsolationLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static org.infinispan.commons.dataconversion.MediaType.*;

@ApplicationScoped
@Startup
public class IspnProducer {
  public static final String OBJECT_CACHE = "OaasObject";
  public static final String INV_NODE_CACHE = "InvNode";
  public static final String CLASS_CACHE = "OaasClass";
  public static final String FUNCTION_CACHE = "OaasFunction";
  private static final Logger logger = LoggerFactory.getLogger(IspnProducer.class);
  @Inject
  EmbeddedCacheManager cacheManager;
  @Inject
  IspnConfig config;

  @Produces
  EmbeddedIspnObjectRepository objectRepository() {
    Cache<String, OaasObject> cache;
    if (!cacheManager.cacheExists(OBJECT_CACHE)) {
      var conf = createDistConfig(config.argConnection(), config.objStore(),
        true,
        OaasObject.class);
      log(OBJECT_CACHE, conf);
      cache = cacheManager.createCache(OBJECT_CACHE, conf);
    } else {
      cache = cacheManager.getCache(OBJECT_CACHE);
    }
    return new EmbeddedIspnObjectRepository(cache.getAdvancedCache());
  }

  @Produces
  EmbeddedIspnClsRepository clsRepository() {
    Cache<String, OaasClass> cache;
    if (!cacheManager.cacheExists(CLASS_CACHE)) {
      var conf = createSimpleConfig(config.argConnection(), config.clsStore(), OaasClass.class);
      log(CLASS_CACHE, conf);
      cache = cacheManager.createCache(CLASS_CACHE, conf);
    } else {
      cache = cacheManager.getCache(CLASS_CACHE);
    }
    return new EmbeddedIspnClsRepository(cache.getAdvancedCache());
  }

  @Produces
  EmbeddedIspnFnRepository fnRepository() {
    Cache<String, OaasFunction> cache;
    if (!cacheManager.cacheExists(FUNCTION_CACHE)) {
      var conf = createSimpleConfig(config.argConnection(), config.fnStore(), OaasFunction.class);
      log(FUNCTION_CACHE, conf);
      cache = cacheManager.createCache(FUNCTION_CACHE, conf);
    } else {
      cache = cacheManager.getCache(FUNCTION_CACHE);
    }
    return new EmbeddedIspnFnRepository(cache.getAdvancedCache());
  }

  @Produces
  EmbeddedIspnInvNodeRepository invNodeRepository() {
    Cache<String, InvocationNode> cache;
    if (!cacheManager.cacheExists(INV_NODE_CACHE)) {
      var conf = createDistConfig(config.argConnection(), config.invNode(),
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


  Configuration createDistConfig(ArgConnectionConfig connectionConfig,
                                 IspnConfig.CacheStore cacheStore,
                                 boolean transactional,
                                 Class<?> valueCls) {
    return new ConfigurationBuilder()
      .clustering()
      .cacheMode(CacheMode.DIST_SYNC)
      .stateTransfer().awaitInitialTransfer(cacheStore.awaitInitialTransfer())
      .encoding()
      .key().mediaType(TEXT_PLAIN_TYPE)
      .encoding()
      .value().mediaType(cacheStore.storageType()==StorageType.HEAP ? APPLICATION_OBJECT_TYPE:APPLICATION_PROTOSTREAM_TYPE)
      .transaction()
      .lockingMode(LockingMode.OPTIMISTIC)
      .transactionMode(transactional ? TransactionMode.TRANSACTIONAL:TransactionMode.NON_TRANSACTIONAL)
      .locking()
      .isolationLevel(IsolationLevel.REPEATABLE_READ)
      .persistence()
      .addStore(ArgCacheStoreConfig.Builder.class)
      .valueCls(valueCls)
      .connectionFactory(new ArgConnectionFactory(connectionConfig))
      .shared(true)
      .segmented(false)
      .ignoreModifications(cacheStore.readOnly())
      .async()
      .enabled(cacheStore.queueSize() > 0)
      .modificationQueueSize(cacheStore.queueSize())
      .failSilently(false)
      .memory()
      .storage(cacheStore.storageType())
      .maxCount(cacheStore.maxCount())
      .whenFull(EvictionStrategy.REMOVE)
      .statistics().enabled(true)
      .build();
  }

  Configuration createSimpleConfig(ArgConnectionConfig connectionConfig,
                                   IspnConfig.CacheStore cacheStore,
                                   Class<?> valueCls) {
    return new ConfigurationBuilder()
      .clustering()
      .cacheMode(CacheMode.LOCAL)
      .encoding()
      .key().mediaType(TEXT_PLAIN_TYPE)
      .encoding()
      .value().mediaType(cacheStore.storageType()==StorageType.HEAP ? APPLICATION_OBJECT_TYPE:APPLICATION_PROTOSTREAM_TYPE)
      .persistence()
      .addStore(ArgCacheStoreConfig.Builder.class)
      .valueCls(valueCls)
      .connectionFactory(new ArgConnectionFactory(connectionConfig))
      .shared(false)
      .ignoreModifications(true)
      .segmented(false)
      .memory()
      .storage(cacheStore.storageType())
      .maxCount(cacheStore.maxCount())
      .whenFull(EvictionStrategy.REMOVE)
      .expiration()
      .lifespan(cacheStore.ttl(), TimeUnit.SECONDS)
      .statistics().enabled(true)
      .build();
  }
}
