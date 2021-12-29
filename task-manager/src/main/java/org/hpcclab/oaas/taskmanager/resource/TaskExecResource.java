package org.hpcclab.oaas.taskmanager.resource;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.task.TaskEvent;
import org.hpcclab.oaas.model.task.TaskExecRequest;
import org.hpcclab.oaas.iface.service.TaskExecutionService;
import org.hpcclab.oaas.taskmanager.TaskManagerConfig;
import org.hpcclab.oaas.taskmanager.service.TaskEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;


public class TaskExecResource implements TaskExecutionService {
  private static final Logger LOGGER = LoggerFactory.getLogger(TaskExecResource.class);

//  @Inject
//  TaskEventManager taskEventManager;
  @Inject
TaskEventManager v2TaskEventManager;
  @Inject
  TaskManagerConfig config;

  @Override
  public Uni<Void> request(TaskExecRequest request) {
    return v2TaskEventManager.submitEventWithTraversal(
      request.getId(),
      config.defaultTraverse(),
      true,
      TaskEvent.Type.CREATE
    );
  }
}
