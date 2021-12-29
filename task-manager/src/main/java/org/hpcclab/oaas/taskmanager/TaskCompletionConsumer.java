package org.hpcclab.oaas.taskmanager;

import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.hpcclab.oaas.taskmanager.service.TaskEventManager;
import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.UUID;

@ApplicationScoped
public class TaskCompletionConsumer {
  private static final Logger LOGGER = LoggerFactory.getLogger( TaskCompletionConsumer.class );

  @Inject
  TaskEventManager taskEventManager;
  @Inject
  TaskEventManager v2TaskEventManager;
  @Remote("TaskCompletion")
  RemoteCache<UUID, TaskCompletion> remoteCache;

  @Incoming("task-completions")
  public Uni<Void> handle(TaskCompletion taskCompletion) {
    remoteCache.put(UUID.fromString(taskCompletion.getId()), taskCompletion);
    if (taskCompletion.getStatus() == TaskStatus.SUCCEEDED) {
//      return taskEventManager.submitCompletionEvent(taskCompletion.getId());
      v2TaskEventManager.submitCompletionEvent(taskCompletion.getId());
      return Uni.createFrom().nullItem();
    } else {
      // TODO retry?
      return Uni.createFrom().nullItem();
    }
  }
}
