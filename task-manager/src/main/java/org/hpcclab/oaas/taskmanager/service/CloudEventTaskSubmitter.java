package org.hpcclab.oaas.taskmanager.service;


import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hpcclab.oaas.model.TaskContext;
import org.hpcclab.oaas.repository.function.TaskSubmitter;
import org.hpcclab.oaas.taskmanager.factory.TaskFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collection;

@ApplicationScoped
public class CloudEventTaskSubmitter implements TaskSubmitter {

  @RestClient
  TaskBrokerService taskBrokerService;
  @Inject
  TaskFactory taskFactory;

  @Override
  public Uni<Void> submit(TaskContext context) {
    var task = taskFactory.genTask(context);
    return taskBrokerService.submitTaskAsync(task.getId(),
      task.getFunction().getName(),
      task);
  }

}
