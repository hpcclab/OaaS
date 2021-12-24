package org.hpcclab.oaas.taskgen;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class TaskCompletionConsumer {
  private static final Logger LOGGER = LoggerFactory.getLogger( TaskCompletionConsumer.class );

  @Inject
  TaskEventManager taskEventManager;

  @Incoming("task-completions")
  public Uni<Void> handle(TaskCompletion taskCompletion) {
    if (taskCompletion.getStatus() == TaskStatus.SUCCEEDED) {
      return taskEventManager.submitCompletionEvent(taskCompletion.getId());
    } else {
      // TODO retry?
      return Uni.createFrom().nullItem();
    }
  }
}
