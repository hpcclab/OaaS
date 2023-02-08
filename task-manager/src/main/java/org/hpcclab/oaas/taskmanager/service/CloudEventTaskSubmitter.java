package org.hpcclab.oaas.taskmanager.service;


import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hpcclab.oaas.model.task.TaskContext;
import org.hpcclab.oaas.invocation.TaskSubmitter;
import org.hpcclab.oaas.invocation.TaskFactory;

import javax.inject.Inject;

//@ApplicationScoped
public class CloudEventTaskSubmitter implements TaskSubmitter {

  @Inject
  @RestClient
  TaskBrokerService taskBrokerService;
  @Inject
  TaskFactory taskFactory;

  @Override
  public Uni<Void> submit(TaskContext context) {
    var task = taskFactory.genTask(context);
    return taskBrokerService.submitTaskAsync(
      task.getId(),
      task.getFuncKey(),
      task);
  }
}
