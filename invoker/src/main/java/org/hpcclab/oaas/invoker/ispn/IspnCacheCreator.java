package org.hpcclab.oaas.invoker.ispn;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invoker.ispn.store.ArgCacheStoreConfig;
import org.hpcclab.oaas.invoker.ispn.store.ArgConnectionFactory;
import org.hpcclab.oaas.model.cls.ClassConfig;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.store.DatastoreConf;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;
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

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.infinispan.commons.dataconversion.MediaType.*;

@ApplicationScoped
public class IspnCacheCreator {
  private static final Logger logger = LoggerFactory.getLogger( IspnCacheCreator.class );
  DatastoreConfRegistry confRegistry = DatastoreConfRegistry.getDefault();
  @Inject
  IspnConfig ispnConfig;
  @Inject
  EmbeddedCacheManager cacheManager;

  public Cache<String, OaasObject> getObjectCache(OaasClass cls) {
    var name = cls.getKey();
    if (cacheManager.cacheExists(name)){
      return cacheManager.getCache(name);
    } else {
      var config = getCacheDistConfig(cls, ispnConfig.objStore(), true);
      return cacheManager.createCache(name, config);
    }
  }

  public Configuration getCacheDistConfig(OaasClass cls,
                                          IspnConfig.CacheStore cacheStore,
                                          boolean transactional) {
    var builder = new ConfigurationBuilder();
    var conf = cls.getConfig();
    if (conf == null) conf = new ClassConfig();
    var structStore = Optional.of(conf)
      .map(ClassConfig::getStructStore)
      .orElse("DEFAULT");
    DatastoreConf datastoreConf = null;
    if (!structStore.equals("NONE")) {
      datastoreConf = confRegistry.getConfMap()
        .get(structStore);
    }
    logger.debug("cls[{}] use store({})", cls.getKey(), structStore);
    builder
      .clustering()
      .cacheMode(CacheMode.DIST_SYNC)
      .hash().numOwners(conf.getReplicas())
      .stateTransfer().awaitInitialTransfer(cacheStore.awaitInitialTransfer())
      .encoding()
      .key().mediaType(TEXT_PLAIN_TYPE)
      .encoding()
      .value().mediaType(cacheStore.storageType()==StorageType.HEAP ? APPLICATION_OBJECT_TYPE:APPLICATION_PROTOSTREAM_TYPE)
      .transaction()
      .lockingMode(LockingMode.OPTIMISTIC)
      .transactionMode(transactional ? TransactionMode.TRANSACTIONAL:TransactionMode.NON_TRANSACTIONAL)
      .locking()
      .isolationLevel(IsolationLevel.READ_COMMITTED)
      .memory()
      .storage(cacheStore.storageType())
      .maxCount(cacheStore.maxCount())
      .whenFull(EvictionStrategy.REMOVE)
      .statistics().enabled(true);
    if (datastoreConf != null) {
      builder.persistence()
        .addStore(ArgCacheStoreConfig.Builder.class)
        .valueCls(OaasObject.class)
        .connectionFactory(new ArgConnectionFactory(datastoreConf))
        .shared(true)
        .segmented(false)
        .ignoreModifications(cacheStore.readOnly())
        .async()
        .enabled(cacheStore.queueSize() > 0)
        .modificationQueueSize(cacheStore.queueSize())
        .failSilently(false);
    }
    return builder.build();
  }

  public Configuration createDistConfig(DatastoreConf datastoreConf,
                                        IspnConfig.CacheStore cacheStore,
                                        boolean transactional,
                                        Class<?> valueCls) {
    var builder = new ConfigurationBuilder();
    builder
      .clustering()
      .cacheMode(CacheMode.DIST_SYNC)
      .hash().numOwners(cacheStore.owner())
      .stateTransfer().awaitInitialTransfer(cacheStore.awaitInitialTransfer())
      .encoding()
      .key().mediaType(TEXT_PLAIN_TYPE)
      .encoding()
      .value().mediaType(cacheStore.storageType()==StorageType.HEAP ? APPLICATION_OBJECT_TYPE:APPLICATION_PROTOSTREAM_TYPE)
      .transaction()
      .lockingMode(LockingMode.OPTIMISTIC)
      .transactionMode(transactional ? TransactionMode.TRANSACTIONAL:TransactionMode.NON_TRANSACTIONAL)
      .locking()
      .isolationLevel(IsolationLevel.READ_COMMITTED)
      .memory()
      .storage(cacheStore.storageType())
      .maxCount(cacheStore.maxCount())
      .whenFull(EvictionStrategy.REMOVE)
      .statistics().enabled(true);
    if (cacheStore.persistentEnabled()) {
      builder.persistence()
        .addStore(ArgCacheStoreConfig.Builder.class)
        .valueCls(valueCls)
        .connectionFactory(new ArgConnectionFactory(datastoreConf))
        .shared(true)
        .segmented(false)
        .ignoreModifications(cacheStore.readOnly())
        .async()
        .enabled(cacheStore.queueSize() > 0)
        .modificationQueueSize(cacheStore.queueSize())
        .failSilently(false);
    }
    return builder.build();
  }

  public Configuration createSimpleConfig(DatastoreConf datastoreConf,
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
      .connectionFactory(new ArgConnectionFactory(datastoreConf))
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
