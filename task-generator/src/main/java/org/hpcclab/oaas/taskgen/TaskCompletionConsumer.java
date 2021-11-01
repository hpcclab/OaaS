package org.hpcclab.oaas.taskgen;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.hpcclab.oaas.entity.task.TaskCompletion;
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
  public void handle(TaskCompletion taskCompletion) {
    if (taskCompletion.getStatus() == TaskCompletion.Status.SUCCEEDED) {
      taskEventManager.submitCompletionEvent(taskCompletion.getId());
    } else {
      // TODO retry?
    }
  }
}
