package org.hpcclab.oaas.invoker.ispn.store;

import com.arangodb.ArangoDBException;
import com.arangodb.async.ArangoCollectionAsync;
import com.arangodb.entity.CollectionPropertiesEntity;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.OverwriteMode;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import mutiny.zero.flow.adapters.AdaptersToFlow;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;
import org.hpcclab.oaas.model.HasKey;
import org.infinispan.commons.configuration.ConfiguredBy;
import org.infinispan.commons.marshall.WrappedByteArray;
import org.infinispan.commons.util.IntSet;
import org.infinispan.marshall.persistence.impl.MarshalledEntryFactoryImpl;
import org.infinispan.persistence.spi.InitializationContext;
import org.infinispan.persistence.spi.MarshallableEntry;
import org.infinispan.persistence.spi.MarshallableEntryFactory;
import org.infinispan.persistence.spi.NonBlockingStore;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;

@ConfiguredBy(ArgCacheStoreConfig.class)

public class ArgCacheStore<T> implements NonBlockingStore<String, T> {
    private static final Logger logger = LoggerFactory.getLogger(ArgCacheStore.class);
    ArangoCollectionAsync collectionAsync;
    MarshallableEntryFactory<String, T> marshallableEntryFactory;

    Class<T> valueCls;
    Function<T, String> keyExtractor;


    @Override
    public CompletionStage<Void> start(InitializationContext ctx) {
        var conf = ctx.getConfiguration();
        if (conf instanceof ArgCacheStoreConfig argCacheStoreConfig) {
            this.valueCls = argCacheStoreConfig.getValuaCls();
            this.keyExtractor = obj -> ((HasKey) obj).getKey();
            collectionAsync = ArgConnectionFactory.getInstance().getArangoDatabase()
                    .collection(ctx.getCache().getName());
        } else {
            throw new IllegalStateException();
        }
        this.marshallableEntryFactory = ctx.getMarshallableEntryFactory();
        return collectionAsync.exists()
                .thenCompose(exist -> {
                    if (Boolean.TRUE.equals(exist)) {
                        return CompletableFuture.completedStage(null);
                    } else {
                        return collectionAsync.create()
                                .thenApply(__ -> null);
                    }
                });
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
    logger.debug("load {} {}", segment, skey);
    return collectionAsync.getDocument(skey, valueCls)
      .thenApply(doc -> {
        if (doc==null)
          return null;
        return marshallableEntryFactory.create(key, doc);
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
    return collectionAsync.insertDocument(entry.getValue(), options)
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
      .group().intoLists().of(1024, Duration.ofMillis(100))
//                .collect().asList()
      .call(list -> Uni.createFrom().completionStage(batchWrite(list)))
      .collect().last()
      .replaceWithVoid();
    var removedUni = Multi.createFrom().publisher(AdaptersToFlow.publisher(removePublisher))
      .flatMap(AdaptersToFlow::publisher)
      .map(this::objToStr)
      .group().intoLists().of(1024, Duration.ofMillis(100))
//                .collect().asList()
      .call(list -> Uni.createFrom().completionStage(batchRemove(list)))
      .collect().last()
      .replaceWithVoid();

    return Uni.join().all(writtenUni, removedUni)
      .andFailFast()
      .replaceWithVoid()
      .subscribeAsCompletionStage();
  }

  CompletionStage<?> batchWrite(List<MarshallableEntry<String, T>> list) {
    logger.debug("batchWrite {}", list.size());
    var valueList = list.stream().map(MarshallableEntry::getValue)
      .toList();
    return collectionAsync.insertDocuments(valueList, new DocumentCreateOptions().overwriteMode(OverwriteMode.replace));
  }

  CompletionStage<?> batchRemove(List<String> list) {
    logger.debug("batchRemove {}", list.size());
    return collectionAsync.deleteDocuments(list);
  }

  @Override
  public CompletionStage<Void> clear() {
    logger.debug("clear");
    return CompletableFuture.completedStage(null);
  }

  @Override
  public CompletionStage<Long> size(IntSet segments) {
    logger.debug("size");
    return collectionAsync.count()
      .thenApply(CollectionPropertiesEntity::getCount);
  }

  @Override
  public CompletionStage<Long> approximateSize(IntSet segments) {
    logger.debug("approximateSize");
    return collectionAsync.count()
      .thenApply(CollectionPropertiesEntity::getCount);
  }

  @Override
  public Publisher<MarshallableEntry<String, T>> publishEntries(IntSet segments, Predicate<? super String> filter, boolean includeValues) {
    logger.debug("publishEntries {} {}", segments, includeValues);
    var query = """
                FOR doc in @@col
                return doc
                """;
    return AdaptersToReactiveStreams.publisher(
      Multi.createFrom()
        .completionStage(() -> collectionAsync.db()
          .query(query, Map.of("@col", collectionAsync.name()), valueCls)
        )
        .flatMap(cursor -> Multi.createFrom()
          .items(cursor.stream())
          .map(val -> marshallableEntryFactory.create(keyExtractor.apply(val), val))
          .filter(entry -> filter.test(entry.getKey()))
        )
    );
  }

  @Override
  public Publisher<String> publishKeys(IntSet segments, Predicate<? super String> filter) {
    var query = """
                FOR doc in @@col
                return doc._key
                """;
    logger.info("publishKeys {}", segments);
    return AdaptersToReactiveStreams.publisher(
      Multi.createFrom()
        .completionStage(collectionAsync.db()
          .query(query, Map.of("@col", collectionAsync.name()), String.class))
        .flatMap(cursor -> Multi.createFrom().items(cursor.streamRemaining()))
    );
  }

  @Override
  public Publisher<MarshallableEntry<String, T>> purgeExpired() {
    return AdaptersToReactiveStreams.publisher(Multi.createFrom().empty());
  }

  private String objToStr(Object key) {
    if (key instanceof WrappedByteArray wrappedByteArray) {
      return new String(wrappedByteArray.getBytes());
    } else {
      return (String) key;
    }
  }
}
