package org.hpcclab.oaas.repository;

import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Uni;
import org.apache.commons.lang3.NotImplementedException;
import org.hpcclab.oaas.model.task.TaskState;
import org.hpcclab.oaas.repository.impl.AbstractIfnpRepository;
import org.infinispan.client.hotrod.RemoteCache;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collection;

@ApplicationScoped
public class TaskStateRepository extends AbstractIfnpRepository<String, TaskState> {
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


  @Override
  protected String extractKey(TaskState taskState) {
    throw new NotImplementedException();
  }
}
