package org.hpcclab.oaas.repository;

import io.quarkus.infinispan.client.Remote;
import org.hpcclab.oaas.model.proto.TaskCompletion;
import org.hpcclab.oaas.model.proto.TaskState;
import org.infinispan.client.hotrod.RemoteCache;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.UUID;

@ApplicationScoped
public class TaskCompletionRepository extends AbstractIfnpRepository<UUID, TaskCompletion>{
  @Inject
  @Remote("TaskCompletion")
  RemoteCache<UUID, TaskCompletion> cache;


  @PostConstruct
  void setup() {
    setRemoteCache(cache);
  }

  @Override
  public String getEntityName() {
    return TaskState.class.getName();
  }
}
