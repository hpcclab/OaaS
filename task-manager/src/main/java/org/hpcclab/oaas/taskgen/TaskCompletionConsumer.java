package org.hpcclab.oaas.taskgen;

import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Uni;
import org.apache.kafka.common.protocol.types.Field;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskStatus;
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
  @Remote("TaskCompletion")
  RemoteCache<UUID, TaskCompletion> remoteCache;

  @Incoming("task-completions")
  public Uni<Void> handle(TaskCompletion taskCompletion) {
    remoteCache.put(UUID.fromString(taskCompletion.getId()), taskCompletion);
    if (taskCompletion.getStatus() == TaskStatus.SUCCEEDED) {
      return taskEventManager.submitCompletionEvent(taskCompletion.getId());
    } else {
      // TODO retry?
      return Uni.createFrom().nullItem();
    }
  }
}
