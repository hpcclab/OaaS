package org.hpcclab.oaas.repository;

import io.quarkus.infinispan.client.Remote;
import org.hpcclab.oaas.model.task.TaskState;
import org.infinispan.client.hotrod.RemoteCache;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class TaskStateRepository extends AbstractIfnpRepository<String, TaskState>{
  @Inject
  @Remote("TaskState")
  RemoteCache<String, TaskState> cache;

  @PostConstruct
  void setup() {
    setRemoteCache(cache);
  }

  @Override
  public String getEntityName() {
    return TaskState.class.getName();
  }
}
