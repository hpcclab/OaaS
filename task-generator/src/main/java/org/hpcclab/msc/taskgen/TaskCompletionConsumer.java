package org.hpcclab.msc.taskgen;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.hpcclab.msc.object.entity.task.TaskCompletion;
import org.hpcclab.msc.object.entity.task.TaskFlow;
import org.hpcclab.msc.object.model.ObjectResourceRequest;
import org.hpcclab.msc.taskgen.repository.TaskCompletionRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class TaskCompletionConsumer {

  @Inject
  TaskCompletionRepository taskCompletionRepo;

  @Incoming("resource-requests")
  public Uni<TaskCompletion> handle(TaskCompletion taskCompletion) {
    return taskCompletionRepo.persist(taskCompletion);
  }

}
