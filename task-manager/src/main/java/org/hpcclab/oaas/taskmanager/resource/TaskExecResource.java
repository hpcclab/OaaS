package org.hpcclab.oaas.taskmanager.resource;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.iface.service.TaskExecutionService;
import org.hpcclab.oaas.model.task.TaskExecRequest;
import org.hpcclab.oaas.taskmanager.service.TaskEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;


public class TaskExecResource implements TaskExecutionService {
  private static final Logger LOGGER = LoggerFactory.getLogger(TaskExecResource.class);

  @Inject
  TaskEventManager taskEventManager;

  @Override
  public Uni<Void> request(TaskExecRequest request) {
    return taskEventManager.submitCreateEvent(request.getId());
  }
}
