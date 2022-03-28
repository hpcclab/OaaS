package org.hpcclab.oaas.taskmanager.service;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.proto.TaskCompletion;
import org.hpcclab.oaas.taskmanager.TaskManagerConfig;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientCacheFailover;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;
import org.infinispan.client.hotrod.event.ClientCacheFailoverEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class TaskCompletionListener {
  private static final Logger LOGGER = LoggerFactory.getLogger( TaskCompletionListener.class );

  @Remote("TaskCompletion")
  RemoteCache<UUID, TaskCompletion> completionCache;
  CacheWatcher watcher;
  @Inject
  TaskManagerConfig config;

  @PostConstruct
  public void setup() {
    if (config.enableCompletionListener()) {
      watcher = new CacheWatcher();
      completionCache.addClientListener(watcher);
    }
  }

  @PreDestroy
  public void cleanup() {
    completionCache.removeClientListener(watcher);
  }

  public Uni<UUID> wait(UUID id) {
    if (!config.enableCompletionListener())
      throw new NoStackException("Completion Listener is not enabled");
    return watcher.wait(id, Duration.ofSeconds(config.blockingTimeout()));
  }


  @ClientListener
  public static class CacheWatcher {

    BroadcastProcessor<UUID> broadcastProcessor = BroadcastProcessor.create();
    ConcurrentHashMap<UUID, AtomicInteger> countingMap = new ConcurrentHashMap<>();

    @ClientCacheEntryCreated
    public void onCreate(ClientCacheEntryCreatedEvent<UUID> e) {
//      LOGGER.debug("onCreate {}, countingMap {}", e, countingMap);
      if (countingMap.containsKey(e.getKey())) {
        broadcastProcessor.onNext(e.getKey());
      }
    }

    @ClientCacheEntryModified
    public void onUpdate(ClientCacheEntryModifiedEvent<UUID> e) {
//      LOGGER.debug("onUpdate {}, countingMap {}", e, countingMap);
      if (countingMap.containsKey(e.getKey())) {
        broadcastProcessor.onNext(e.getKey());
      }
    }

    @ClientCacheFailover
    public void onFail(ClientCacheFailoverEvent event) {
      broadcastProcessor.onError(new NoStackException(HttpResponseStatus.BAD_GATEWAY
        .code()));
      broadcastProcessor = BroadcastProcessor.create();
    }

    public Uni<UUID> wait(UUID id, Duration timeout) {
      LOGGER.debug("start wait for {}", id);
      countingMap.computeIfAbsent(id, key -> new AtomicInteger())
        .incrementAndGet();
      return broadcastProcessor
        .filter(event -> event.equals(id))
        .toUni()
        .ifNoItem().after(timeout)
        .recoverWithItem((UUID) null)
        .eventually(() -> {
          var atomicInteger = countingMap.get(id);
          var i = atomicInteger.decrementAndGet();
          if (i==0) countingMap.remove(id);
        });
    }
  }
}
