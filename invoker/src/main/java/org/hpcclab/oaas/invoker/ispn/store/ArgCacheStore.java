package org.hpcclab.oaas.invoker.ispn.store;

import com.arangodb.ArangoCollectionAsync;
import com.arangodb.ArangoCursorAsync;
import com.arangodb.ArangoDBException;
import com.arangodb.entity.CollectionPropertiesEntity;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.OverwriteMode;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.MultiEmitter;
import mutiny.zero.flow.adapters.AdaptersToFlow;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;
import org.hpcclab.oaas.model.HasKey;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;
import org.infinispan.commons.configuration.ConfiguredBy;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.util.IntSet;
import org.infinispan.container.versioning.SimpleClusteredVersion;
import org.infinispan.encoding.DataConversion;
import org.infinispan.metadata.EmbeddedMetadata;
import org.infinispan.metadata.impl.PrivateMetadata;
import org.infinispan.persistence.spi.InitializationContext;
import org.infinispan.persistence.spi.MarshallableEntry;
import org.infinispan.persistence.spi.MarshallableEntryFactory;
import org.infinispan.persistence.spi.NonBlockingStore;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@ConfiguredBy(ArgCacheStoreConfig.class)
public class ArgCacheStore<T, S> implements NonBlockingStore<String, T> {
  private static final Logger logger = LoggerFactory.getLogger(ArgCacheStore.class);
  ArangoCollectionAsync collectionAsync;
  MarshallableEntryFactory<String, T> marshallableEntryFactory;
  String name;
  Class<T> valueCls;
  Class<S> storeCls;
  ValueMapper<T, S> valueMapper;
  Function<T, String> keyExtractor;
  DataConversion valueDataConversion;
  DataConversion keyDataConversion;

  @Override
  public CompletionStage<Void> start(InitializationContext ctx) {
    valueDataConversion = ctx.getCache().getAdvancedCache().getValueDataConversion()
      .withRequestMediaType(MediaType.APPLICATION_OBJECT);
    keyDataConversion = ctx.getCache().getAdvancedCache().getKeyDataConversion();
    name = ctx.getCache().getAdvancedCache().getName();
    var conf = ctx.getConfiguration();
    logger.debug("starting {} {}", name, conf);
    if (conf instanceof ArgCacheStoreConfig argCacheStoreConfig) {
      try {
        this.valueCls = argCacheStoreConfig.getValueCls();
        this.storeCls = argCacheStoreConfig.getStoreCls();
        this.valueMapper = (ValueMapper<T, S>) argCacheStoreConfig.getValueMapper()
          .getConstructor()
          .newInstance();
        this.keyExtractor = obj -> ((HasKey<String>) obj).getKey();
        String storeConfName = argCacheStoreConfig.getStoreConfName();
        ArgConnectionFactory factory = new ArgConnectionFactory(DatastoreConfRegistry.getDefault().get(storeConfName));
        collectionAsync = factory.getConnection(name);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    } else {
      throw new IllegalStateException();
    }
    this.marshallableEntryFactory = ctx.getMarshallableEntryFactory();
    if (((ArgCacheStoreConfig) conf).isAutoCreate()) {
      return collectionAsync.exists()
        .thenCompose(exist -> {
          if (Boolean.TRUE.equals(exist)) {
            return CompletableFuture.completedStage(null);
          } else {
            return collectionAsync.create()
              .thenApply(__ -> null);
          }
        });
    } else {
      return CompletableFuture.completedStage(null);
    }
  }


  @Override
  public Set<Characteristic> characteristics() {
    return EnumSet.of(
      Characteristic.SHAREABLE,
      Characteristic.BULK_READ
    );
  }


  @Override
  public CompletionStage<Void> stop() {
    return CompletableFuture.runAsync(() -> collectionAsync.db().arango().shutdown());
  }

  @Override
  public CompletionStage<MarshallableEntry<String, T>> load(int segment, Object key) {
    var skey = objToStr(key);
    logger.debug("[{}]load {} {}", name, segment, skey);
    return collectionAsync.getDocument(skey, storeCls)
      .thenApply(doc -> {
        if (doc==null)
          return null;
        T storeVal = valueMapper.mapToCStore(doc);
//        logger.debug("[{}]loaded {}", name, storeVal);
        return marshallableEntryFactory.create(key,
          valueDataConversion.toStorage(storeVal),
          new EmbeddedMetadata.Builder()
            .build(),
          new PrivateMetadata.Builder()
            .entryVersion(new SimpleClusteredVersion(-1, -1))
            .build(),
          -1,
          -1
        );
      });
  }


  @Override
  public CompletionStage<Boolean> isAvailable() {
    return collectionAsync.db().exists();
  }

  @Override
  public CompletionStage<Boolean> containsKey(int segment, Object key) {
    var skey = objToStr(key);
    logger.debug("containsKey {} {}", segment, skey);
    return collectionAsync.documentExists(skey);
  }

  @Override
  public CompletionStage<Void> write(int segment, MarshallableEntry<? extends String, ? extends T> entry) {
    logger.debug("write {} {}", segment, entry.getKey());
    var options = new DocumentCreateOptions().overwriteMode(OverwriteMode.replace)
      .silent(true);
    T val = (T) valueDataConversion.fromStorage(entry.getValue());
    return collectionAsync.insertDocument(valueMapper.mapToDb(val), options)
      .thenApply(__ -> null);
  }

  @Override
  public CompletionStage<Boolean> delete(int segment, Object key) {
    var skey = objToStr(key);
    logger.debug("delete {} {}", segment, skey);
    return collectionAsync.deleteDocument(skey)
      .thenApply(doc -> doc.getId()!=null)
      .exceptionally(err -> {
        if (err instanceof CompletionException completionException &&
          completionException.getCause() instanceof ArangoDBException dbException &&
          dbException.getResponseCode().equals(404)) {
          return false;
        }
        throw new RuntimeException(err);
      });
  }

  @Override
  public CompletionStage<Void> batch(int publisherCount, Publisher<SegmentedPublisher<Object>> removePublisher, Publisher<SegmentedPublisher<MarshallableEntry<String, T>>> writePublisher) {

    var writtenUni = Multi.createFrom().publisher(AdaptersToFlow.publisher(writePublisher))
      .flatMap(AdaptersToFlow::publisher)
//      .group().intoLists().of(65536, Duration.ofMillis(100))
      .collect().asList()
      .call(list -> Uni.createFrom().completionStage(batchWrite(list)))
//      .collect().last()
      .replaceWithVoid();
    var removedUni = Multi.createFrom().publisher(AdaptersToFlow.publisher(removePublisher))
      .flatMap(AdaptersToFlow::publisher)
      .map(this::objToStr)
//      .group().intoLists().of(65536, Duration.ofMillis(100))
      .collect().asList()
      .call(list -> Uni.createFrom().completionStage(batchRemove(list)))
//      .collect().last()
      .replaceWithVoid();

    return Uni.join().all(writtenUni, removedUni)
      .andCollectFailures()
      .replaceWithVoid()
      .subscribeAsCompletionStage();
  }

  CompletionStage<?> batchWrite(List<MarshallableEntry<String, T>> list) {
    if (list.isEmpty())
      return CompletableFuture.completedStage(null);
    logger.debug("[{}]batchWrite {}", name, list.size());

    var valueList = list.stream().map(MarshallableEntry::getValue)
      .map(v -> (T) valueDataConversion.fromStorage(v))
      .map(valueMapper::mapToDb)
      .toList();

    return collectionAsync.insertDocuments(valueList, new DocumentCreateOptions().overwriteMode(OverwriteMode.replace));
  }

  CompletionStage<?> batchRemove(List<String> list) {
    if (list.isEmpty())
      return CompletableFuture.completedStage(null);
    logger.debug("[{}]batchRemove {}", name, list.size());
    return collectionAsync.deleteDocuments(list);
  }

  @Override
  public CompletionStage<Void> clear() {
    logger.debug("[{}]clear", name);
    return CompletableFuture.completedStage(null);
  }

  @Override
  public CompletionStage<Long> size(IntSet segments) {
    logger.debug("[{}]size", name);
    return collectionAsync.count()
      .thenApply(CollectionPropertiesEntity::getCount);
  }

  @Override
  public CompletionStage<Long> approximateSize(IntSet segments) {
    logger.debug("[{}]approximateSize", name);
    return collectionAsync.count()
      .thenApply(CollectionPropertiesEntity::getCount);
  }

  @Override
  public Publisher<MarshallableEntry<String, T>> publishEntries(IntSet segments, Predicate<? super String> filter, boolean includeValues) {
    if (!segments.contains(0))
      return AdaptersToReactiveStreams.publisher(Multi.createFrom().empty());
    logger.debug("[{},{}]publishEntries {} {}", name, collectionAsync.name(), segments, includeValues);
    var query = """
      FOR doc in %s
      return doc
      """.formatted(collectionAsync.name());
    var multi = toMulti(() -> collectionAsync.db()
      .query(query, storeCls)
    )
      .map(storeVal -> {
          var cVal = valueMapper.mapToCStore(storeVal);
          return marshallableEntryFactory.create(
            keyDataConversion.toStorage(keyExtractor.apply(cVal)),
            valueDataConversion.toStorage(cVal));
        }
      );
    return AdaptersToReactiveStreams.publisher(multi);
  }

  @Override
  public Publisher<String> publishKeys(IntSet segments, Predicate<? super String> filter) {
    if (!segments.contains(0))
      return AdaptersToReactiveStreams.publisher(Multi.createFrom().empty());
    var query = """
      FOR doc in @@col
      return doc._key
      """;
    logger.debug("[{}]publishKeys {}", name, segments);
    return AdaptersToReactiveStreams.publisher(
      toMulti(() -> collectionAsync.db()
        .query(query, String.class, Map.of("@col", collectionAsync.name()))
      )
    );
  }

  <D> Multi<D> toMulti(Supplier<CompletionStage<ArangoCursorAsync<D>>> supplier) {
    return Multi.createFrom().emitter(emitter -> pipe(supplier.get(), emitter));
  }

  <D> void pipe(CompletionStage<ArangoCursorAsync<D>> stage, MultiEmitter<? super D> emitter) {
    stage.whenComplete((cursorAsync, throwable) -> {
      if (throwable!=null) emitter.fail(throwable);
      for (D t : cursorAsync.getResult()) {
        emitter.emit(t);
      }
      if (Boolean.TRUE.equals(cursorAsync.hasMore())) {
        pipe(cursorAsync.nextBatch(), emitter);
      } else {
        cursorAsync.close();
        emitter.complete();
      }
    });
  }

  @Override
  public Publisher<MarshallableEntry<String, T>> purgeExpired() {
    return AdaptersToReactiveStreams.publisher(Multi.createFrom().empty());
  }

  private String objToStr(Object key) {
    if (key instanceof String s) {
      return s;
    }
    return (String) keyDataConversion.fromStorage(key);
  }
}
