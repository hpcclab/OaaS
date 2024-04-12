package org.hpcclab.oaas.invoker.ispn;

import io.smallrye.mutiny.Uni;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumer;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.lookup.HashRegistry;
import org.hpcclab.oaas.invoker.ispn.repo.EIspnObjectRepository;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.repository.ObjectRepoManager;
import org.infinispan.AdvancedCache;
import org.infinispan.commons.util.IntSet;
import org.infinispan.commons.util.IntSets;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.TopologyChanged;
import org.infinispan.notifications.cachelistener.event.TopologyChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

public class SegmentCoordinator {
  private static final Logger logger = LoggerFactory.getLogger( SegmentCoordinator.class );
  final String topic;
  final int port;
  final OClass cls;
  final KafkaConsumer<?, ?> consumer;
  final HashRegistry registry;
  final SegmentObserver segmentObserver;
  final ObjectRepoManager objectRepoManager;
  AdvancedCache<?, ?> cache;
  IntSet localParts = IntSets.immutableEmptySet();
  Runnable runnable;

  public SegmentCoordinator(OClass cls,
                            ObjectRepoManager objectRepoManager,
                            KafkaConsumer<?, ?> consumer,
                            HashRegistry registry,
                            InvokerConfig config) {
    this.cls = cls;
    this.consumer = consumer;
    this.registry = registry;
    this.objectRepoManager = objectRepoManager;
    topic = config.invokeTopicPrefix() + cls.getKey();
    port = ConfigProvider.getConfig().getValue("quarkus.http.port", Integer.class);
    segmentObserver = new SegmentObserver();
  }

  public void init(Runnable runnable) {
    this.runnable = runnable;
    var repo = (EIspnObjectRepository) objectRepoManager.getOrCreate(cls);
    this.cache = repo.getCache();
    cache.addListener(segmentObserver);
  }

  public void close() {
    cache.removeListener(segmentObserver);
  }

  public OClass getCls() {
    return cls;
  }

  public Uni<Void> updateParts() {
    var topology = cache
      .getDistributionManager()
      .getCacheTopology();
    return assignParts(topology.getLocalPrimarySegments());
  }

  public Uni<Void> assignParts(IntSet parts) {
    localParts = parts;
    return consumer.assignment()
      .map(tp -> tp.stream()
        .map(TopicPartition::getPartition)
        .collect(Collectors.toSet())
      )
      .flatMap(currentParts -> {
        if (!currentParts.equals(parts)) {
          logger.info("[{}] assign partitions {}", topic, parts);
          return consumer.assign(parts.stream()
            .map(p -> new TopicPartition(topic, p))
            .collect(Collectors.toSet())
          );
        }
        return Uni.createFrom().nullItem();
      })
      .invoke(__ -> registry.initLocal(cache.getCacheManager()))
      .call(__ ->registry.updateManaged(cls.getKey(), cache, port))
      .invoke(__ -> {
        if (!localParts.isEmpty()) runnable.run();
      });
  }

  @Listener
  public class SegmentObserver {
    @TopologyChanged
    public void topologyChanged(TopologyChangedEvent<String, String> event) {
      updateParts().await().indefinitely();
    }
  }

}
