package org.hpcclab.oaas.taskmanager.service;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.impl.OaasObjectRepository;
import org.hpcclab.oaas.taskmanager.TaskManagerConfig;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientCacheFailover;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;
import org.infinispan.client.hotrod.event.ClientCacheFailoverEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class ObjectCompletionListener {
  private static final Logger LOGGER = LoggerFactory.getLogger( ObjectCompletionListener.class );

//  @Remote("TaskCompletion")
//  RemoteCache<UUID, TaskCompletion> completionCache;
  @Inject
  OaasObjectRepository objectRepo;
  RemoteCache<String, OaasObject> remoteCache;
  CacheWatcher watcher;
  @Inject
  TaskManagerConfig config;

  @PostConstruct
  public void setup() {
    remoteCache = objectRepo.getRemoteCache();
    if (config.enableCompletionListener()) {
      watcher = new CacheWatcher();
      objectRepo.getRemoteCache().addClientListener(watcher);
    }
  }

  @PreDestroy
  public void cleanup() {
    remoteCache.removeClientListener(watcher);
  }

  public Uni<String> wait(String id) {
    if (!config.enableCompletionListener())
      throw new NoStackException("Completion Listener is not enabled");
    return watcher.wait(id, Duration.ofSeconds(config.blockingTimeout()));
  }


  @ClientListener
  public static class CacheWatcher {

    BroadcastProcessor<String> broadcastProcessor = BroadcastProcessor.create();
    ConcurrentHashMap<String, AtomicInteger> countingMap = new ConcurrentHashMap<>();

//    @ClientCacheEntryCreated
//    public void onCreate(ClientCacheEntryCreatedEvent<UUID> e) {
////      LOGGER.debug("onCreate {}, countingMap {}", e, countingMap);
//      if (countingMap.containsKey(e.getKey())) {
//        broadcastProcessor.onNext(e.getKey());
//      }
//    }

    @ClientCacheEntryModified
    public void onUpdate(ClientCacheEntryModifiedEvent<String> e) {
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

    public Uni<String> wait(String id, Duration timeout) {
      LOGGER.debug("start wait for {}", id);
      countingMap.computeIfAbsent(id, key -> new AtomicInteger())
        .incrementAndGet();
      return broadcastProcessor
        .filter(event -> event.equals(id))
        .toUni()
        .ifNoItem().after(timeout)
        .recoverWithItem((String) null)
        .eventually(() -> {
          var atomicInteger = countingMap.get(id);
          var i = atomicInteger.decrementAndGet();
          if (i==0) countingMap.remove(id);
        });
    }
  }
}
