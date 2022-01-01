package org.hpcclab.oaas.taskmanager.resource;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskEvent;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.hpcclab.oaas.repository.OaasObjectRepository;
import org.hpcclab.oaas.taskmanager.TaskManagerConfig;
import org.hpcclab.oaas.taskmanager.service.TaskEventManager;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/contents")
public class BlockingContentResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(BlockingContentResource.class);

  CacheWatcher watcher;

  @Remote("TaskCompletion")
  RemoteCache<UUID, TaskCompletion> completionCache;
  @Inject
  OaasObjectRepository objectRepo;
  @Inject
  TaskEventManager taskEventManager;
  @Inject
  TaskManagerConfig config;

  @PostConstruct
  public void setup() {
    watcher = new CacheWatcher();
    completionCache.addClientListener(watcher);
  }

  @PreDestroy
  public void cleanup() {
    completionCache.removeClientListener(watcher);
  }

  @GET
  @Path("{objectId}/{filePath:.*}")
  public Uni<Response> get(String objectId,
                           String filePath) {
    var id = UUID.fromString(objectId);
    return Uni.createFrom().completionStage(completionCache.getAsync(id))
      .onItem().ifNull()
      .switchTo(() -> execAndWait(id, filePath))
      .flatMap(taskCompletion -> {
        if (taskCompletion==null) {
          return Uni.createFrom().item(
            Response.status(HttpResponseStatus.GATEWAY_TIMEOUT.code()).build()
          );
        }
        if (taskCompletion.getStatus()!=TaskStatus.SUCCEEDED) {
          return Uni.createFrom().item(
            Response.status(HttpResponseStatus.FAILED_DEPENDENCY.code())
              .build());
        }
        return objectRepo.getAsync(id)
          .map(obj -> {
            var baseUrl = obj.getState().getBaseUrl();
            if (!baseUrl.endsWith("/"))
              baseUrl += '/';
            return Response.status(HttpResponseStatus.FOUND.code())
              .location(URI.create(baseUrl).resolve(filePath))
              .build();
          });
      });
  }

  private Uni<TaskCompletion> execAndWait(UUID id, String filePath) {
    var uni1 = watcher.wait(id, Duration.ofSeconds(30));
//    var uni2 = taskEventManager.submitEventWithTraversal(
//      id.toString(),
//      config.defaultTraverse(),
//      true,
//      TaskEvent.Type.CREATE
//    );
    var uni2 = taskEventManager.submitCreateEvent(id.toString());
    return Uni.combine().all().unis(uni1, uni2)
      .asTuple()
      .flatMap(event -> Uni.createFrom().completionStage(completionCache.getAsync(id)));
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

//    @ClientCacheFailover
//    public void onFail(ClientCacheFailoverEvent e) {
//
//    }

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
