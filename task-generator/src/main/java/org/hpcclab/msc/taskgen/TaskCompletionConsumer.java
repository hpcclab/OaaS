package org.hpcclab.msc.taskgen;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.hpcclab.msc.object.entity.task.TaskCompletion;
import org.hpcclab.msc.taskgen.repository.TaskCompletionRepository;
import org.hpcclab.msc.taskgen.repository.TaskFlowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class TaskCompletionConsumer {
  private static final Logger LOGGER = LoggerFactory.getLogger( TaskCompletionConsumer.class );

  @Inject
  TaskCompletionRepository taskCompletionRepo;
  @Inject
  TaskFlowRepository taskFlowRepo;
  @Inject
  TaskHandler taskHandler;

  @Incoming("task-completions")
  public Uni<TaskCompletion> handle(TaskCompletion taskCompletion) {
    return taskCompletionRepo.persist(taskCompletion)
      .call(this::submitNextTask);
  }

  private Uni<Void> submitNextTask(TaskCompletion taskCompletion) {
    return taskFlowRepo.find("{'prerequisiteTasks':?1}", taskCompletion.getId())
      .stream()
      .invoke(flow -> LOGGER.debug("Checking on flow {}", flow.getId()))
      .onItem().transformToUniAndConcatenate(
        flow -> taskHandler.checkSubmittable(flow)
      )
      .collect().last()
      .map(b -> null);
  }
}
