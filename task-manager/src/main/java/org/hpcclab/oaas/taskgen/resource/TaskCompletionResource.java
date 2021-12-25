package org.hpcclab.oaas.taskgen.resource;

import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.vertx.core.impl.ConcurrentHashSet;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;
import org.jboss.resteasy.reactive.RestSseElementType;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/task-completions")
public class TaskCompletionResource {

  CacheWatcher watcher;
  @Remote("TaskCompletion")
  RemoteCache<UUID, TaskCompletion> remoteCache;

  @PostConstruct
  public void setup() {
    watcher = new CacheWatcher();
    remoteCache.addClientListener(watcher);
  }

  @GET
  @Produces(MediaType.SERVER_SENT_EVENTS)
  @RestSseElementType(MediaType.APPLICATION_JSON)
  public Multi<UUID> stream() {
    return watcher.broadcastProcessor;
  }

  @ClientListener
  public static class CacheWatcher {

    BroadcastProcessor<UUID> broadcastProcessor = BroadcastProcessor.create();
    ConcurrentHashSet<UUID> set= new ConcurrentHashSet<>();

    @ClientCacheEntryCreated
    public void create(ClientCacheEntryCreatedEvent<UUID> e) {
      broadcastProcessor.onNext(e.getKey());
    }

    @ClientCacheEntryModified
    public void update(ClientCacheEntryModifiedEvent<UUID> e) {
      broadcastProcessor.onNext(e.getKey());
    }
  }
}
