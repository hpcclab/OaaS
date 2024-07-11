package org.hpcclab.oaas.invoker.ispn;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.ispn.store.ArgCacheStoreConfigBuilder;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.cls.OClassConfig;
import org.hpcclab.oaas.model.object.GOObject;
import org.hpcclab.oaas.model.object.JOObject;
import org.hpcclab.oaas.repository.store.DatastoreConf;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;
import org.infinispan.Cache;
import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.configuration.cache.*;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static org.infinispan.commons.dataconversion.MediaType.*;

@ApplicationScoped
public class IspnCacheCreator {
  private static final Logger logger = LoggerFactory.getLogger(IspnCacheCreator.class);
  final InvokerConfig invokerConfig;
  final IspnConfig ispnConfig;
  final EmbeddedCacheManager cacheManager;
  DatastoreConfRegistry confRegistry = DatastoreConfRegistry.getDefault();

  @Inject
  public IspnCacheCreator(InvokerConfig invokerConfig,
                          IspnConfig ispnConfig,
                          EmbeddedCacheManager cacheManager) {
    this.invokerConfig = invokerConfig;
    this.ispnConfig = ispnConfig;
    this.cacheManager = cacheManager;
  }

  private static <V, S> void addStore(OClass cls,
                                      IspnConfig.CacheStore cacheStore,
                                      DatastoreConf datastoreConf,
                                      Class<V> valueCls,
                                      Class<S> storeCls,
                                      ConfigurationBuilder builder) {
    var storeBuilder = builder.persistence()
      .addStore(ArgCacheStoreConfigBuilder.class)
      .valueCls(valueCls)
      .storeCls(storeCls)
      .valueMapper(GJValueMapper.class)
      .autoCreate(true)
      .storeConfName(datastoreConf.name())
      .shared(true)
      .segmented(false)
      .ignoreModifications(cacheStore.readOnly());
    if (!cls.getConfig().isWriteThrough()) {
      storeBuilder.async()
        .enabled(cacheStore.queueSize() > 0)
        .modificationQueueSize(cacheStore.queueSize())
        .failSilently(false);
    }
  }

  public Cache<String, GOObject> getObjectCache(OClass cls) {
    DatastoreConf datastoreConf = confRegistry
      .getOrDefault(Optional.ofNullable(cls.getConfig())
        .map(OClassConfig::getStructStore)
        .orElse(DatastoreConfRegistry.DEFAULT));
    Configuration config;
    if (cls.getConfig().isReplicated()) {
      config = getCacheRepConfig(cls,
        ispnConfig.objStore(),
        datastoreConf,
        GOObject.class,
        JOObject.class,
        false);
    } else {
      config = getCacheDistConfig(cls,
        ispnConfig.objStore(),
        datastoreConf,
        GOObject.class,
        JOObject.class,
        false);
    }
    logger.debug("create cache {} {}", cls.getKey(), config);
    return cacheManager.administration()
      .withFlags(CacheContainerAdmin.AdminFlag.VOLATILE)
      .getOrCreateCache(cls.getKey(), config);
  }

  public <V> Cache<String, V> createReplicateCache(String name,
                                                   boolean awaitStateTransfer,
                                                   int maxCount) {
    var cb = new ConfigurationBuilder()
      .clustering()
      .cacheMode(CacheMode.REPL_ASYNC)
      .stateTransfer()
      .awaitInitialTransfer(awaitStateTransfer)
      .encoding()
      .key().mediaType(TEXT_PLAIN_TYPE)
      .encoding()
      .value().mediaType(APPLICATION_OBJECT_TYPE)
      .memory()
      .storage(StorageType.HEAP)
      .maxCount(maxCount)
      .whenFull(EvictionStrategy.REMOVE)
      .statistics().enabled(false);
    return cacheManager.administration()
      .withFlags(CacheContainerAdmin.AdminFlag.VOLATILE)
      .getOrCreateCache(name, cb.build());
  }

  public <V, S> Configuration getCacheRepConfig(OClass cls,
                                                IspnConfig.CacheStore cacheStore,
                                                DatastoreConf datastoreConf,
                                                Class<V> valueCls,
                                                Class<S> storeCls,
                                                boolean transactional) {
    var builder = new ConfigurationBuilder();
    var conf = cls.getConfig();
    if (conf==null) conf = new OClassConfig();

    builder
      .clustering()
      .cacheMode(cacheStore.async() ? CacheMode.REPL_ASYNC:CacheMode.REPL_SYNC).hash()
      .numSegments(conf.getPartitions())
      .stateTransfer()
      .awaitInitialTransfer(cacheStore.awaitInitialTransfer())
      .chunkSize(cacheStore.transferChuckSize())
      .fetchInMemoryState(cacheStore.fetchInMemoryState())
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
      .maxSize(cacheStore.maxSize().orElse(null))
      .maxCount(cacheStore.maxCount().orElse(-1L))
      .whenFull(EvictionStrategy.REMOVE)
      .statistics().enabled(true);
    if (datastoreConf==null || cls.getConstraints().ephemeral())
      return builder.build();

    addStore(cls, cacheStore, datastoreConf, valueCls, storeCls, builder);
    return builder.build();
  }

  public <V, S> Configuration getCacheDistConfig(OClass cls,
                                                 IspnConfig.CacheStore cacheStore,
                                                 DatastoreConf datastoreConf,
                                                 Class<V> valueCls,
                                                 Class<S> storeCls,
                                                 boolean transactional) {
    var builder = new ConfigurationBuilder();
    var conf = cls.getConfig();
    if (conf==null) conf = new OClassConfig();

    builder
      .clustering()
      .cacheMode(cacheStore.async() ? CacheMode.DIST_ASYNC:CacheMode.DIST_SYNC)
      .hash().numOwners(ispnConfig.objStore().owner())
      .numSegments(conf.getPartitions())
      .stateTransfer()
      .awaitInitialTransfer(cacheStore.awaitInitialTransfer())
      .chunkSize(cacheStore.transferChuckSize())
      .fetchInMemoryState(cacheStore.fetchInMemoryState())
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
      .maxSize(cacheStore.maxSize().orElse(null))
      .maxCount(cacheStore.maxCount().orElse(-1L))
      .whenFull(EvictionStrategy.REMOVE)
      .statistics().enabled(true);
    if (datastoreConf==null || cls.getConstraints().ephemeral())
      return builder.build();

    addStore(cls, cacheStore, datastoreConf, valueCls, storeCls, builder);
    return builder.build();
  }

}
