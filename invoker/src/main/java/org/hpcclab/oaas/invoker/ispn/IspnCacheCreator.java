package org.hpcclab.oaas.invoker.ispn;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.ispn.store.ArgCacheStoreConfig;
import org.hpcclab.oaas.invoker.ispn.store.ArgConnectionFactory;
import org.hpcclab.oaas.invoker.ispn.store.ValueMapper;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.cls.OClassConfig;
import org.hpcclab.oaas.model.object.GOObject;
import org.hpcclab.oaas.model.object.JOObject;
import org.hpcclab.oaas.model.object.JsonBytes;
import org.hpcclab.oaas.repository.store.DatastoreConf;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;
import org.infinispan.Cache;
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


  public Cache<String, GOObject> getObjectCache(OClass cls) {
    var name = cls.getKey();
    if (cacheManager.cacheExists(name)) {
      return cacheManager.getCache(name);
    } else {
      DatastoreConf datastoreConf = confRegistry
        .getOrDefault(Optional.ofNullable(cls.getConfig())
          .map(OClassConfig::getStructStore)
          .orElse(DatastoreConfRegistry.DEFAULT));
      var config = getCacheDistConfig(cls,
        ispnConfig.objStore(),
        datastoreConf,
        GOObject.class,
        JOObject.class,
        new GJValueMapper(),
        false);
      return cacheManager.createCache(name, config);
    }
  }

  public <V> Cache<String, V> createReplicateCache(String name,
                                                   boolean awaitStateTransfer,
                                                   int maxCount) {
    if (cacheManager.cacheExists(name)) {
      return cacheManager.getCache(name);
    }
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
    return cacheManager.createCache(name, cb.build());
  }

  public <V, S> Configuration getCacheDistConfig(OClass cls,
                                                 IspnConfig.CacheStore cacheStore,
                                                 DatastoreConf datastoreConf,
                                                 Class<V> valueCls,
                                                 Class<S> storeCls,
                                                 ValueMapper<V, S> valueMapper,
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
    if (datastoreConf!=null) {
      builder.persistence()
        .addStore(ArgCacheStoreConfig.Builder.class)
        .valueCls(valueCls)
        .storeCls(storeCls)
        .valueMapper(valueMapper)
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

  static class GJValueMapper implements ValueMapper<GOObject, JOObject> {
    @Override
    public JOObject mapToDb(GOObject goObject) {
      return new JOObject(goObject.getMeta(), goObject.getData().getNode());
    }

    @Override
    public GOObject mapToCStore(JOObject joObject) {
      return new GOObject(joObject.getMeta(), new JsonBytes(joObject.getData()));
    }
  }
}
