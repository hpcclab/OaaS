package org.hpcclab.oaas.taskgen.resource;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskEvent;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.hpcclab.oaas.taskgen.service.TaskEventManager;
import org.hpcclab.oaas.taskgen.TaskManagerConfig;
import org.hpcclab.oaas.taskgen.service.V2TaskEventManager;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
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
  private static final Logger LOGGER = LoggerFactory.getLogger( BlockingContentResource.class );

  CacheWatcher watcher;

  @Remote("TaskCompletion")
  RemoteCache<UUID, TaskCompletion> remoteCache;

//  @Inject
//  TaskEventManager taskEventManager;
  @Inject
  V2TaskEventManager v2TaskEventManager;
  @Inject
  TaskManagerConfig config;

  @PostConstruct
  public void setup() {
    watcher = new CacheWatcher();
    remoteCache.addClientListener(watcher);
  }

  @GET
  @Path("{objectId}/{filePath:.*}")
  public Uni<Response> get(String objectId,
                                     String filePath) {
    var id = UUID.fromString(objectId);
    return Uni.createFrom().completionStage(remoteCache.getAsync(id))
      .onItem().ifNull()
      .switchTo(() -> execAndWait(id, filePath))
      .map(taskCompletion ->  {
        if (taskCompletion == null) {
          return Response.status(HttpResponseStatus.GATEWAY_TIMEOUT.code()).build();
        }
        if (taskCompletion.getStatus() == TaskStatus.SUCCEEDED) {
          if (!taskCompletion.getResourceUrl().endsWith("/"))
            taskCompletion.setResourceUrl(taskCompletion.getResourceUrl()+'/');
          return Response.status(HttpResponseStatus.FOUND.code())
            .location(URI.create(taskCompletion.getResourceUrl()).resolve(filePath))
            .build();
        }
        return Response.status(HttpResponseStatus.FAILED_DEPENDENCY.code())
          .build();
      });
  }

  private Uni<TaskCompletion> execAndWait(UUID id, String filePath) {
    var uni1 = watcher.wait(id, Duration.ofSeconds(30));
    var uni2 = v2TaskEventManager.submitEventWithTraversal(
      id.toString(),
      config.defaultTraverse(),
      true,
      TaskEvent.Type.CREATE
    );
    return Uni.combine().all().unis(uni1, uni2)
      .asTuple()
      .flatMap(event -> Uni.createFrom().completionStage(remoteCache.getAsync(id)));
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
          if (i == 0) countingMap.remove(id);
        });
    }
  }
}
