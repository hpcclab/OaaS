package org.hpcclab.oaas.taskmanager.service;

import io.micrometer.core.annotation.Timed;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskEvent;
import org.hpcclab.oaas.repository.AggregateRepository;
import org.hpcclab.oaas.repository.OaasObjectRepository;
import org.hpcclab.oaas.taskmanager.factory.TaskFactory;
import org.hpcclab.oaas.taskmanager.factory.TaskEventFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TaskEventManager {
  private static final Logger LOGGER = LoggerFactory.getLogger( TaskEventManager.class );

  @Inject
  TaskEventProcessor taskEventProcessor;
  @Inject
  OaasObjectRepository objectRepo;
  @Inject
  AggregateRepository aggregateRepo;
  @Inject
  TaskFactory taskFactory;
  @Inject
  TaskEventFactory taskEventFactory;
  @Inject
  Vertx vertx;

  @Timed(value = "processEvent", percentiles={0.5,0.75,0.95,0.99})
  public Uni<Void> submitEventWithTraversal(String objId,
                                            int traverse,
                                            boolean exec,
                                            TaskEvent.Type type) {

    Uni<Void> uni = Uni.createFrom().item(() -> {
      var events = createEventWithTraversal(objId, traverse, exec,type);
      taskEventProcessor.processEvent(events);
      return null;
    });
    return vertx.executeBlocking(uni);
  }

  public List<TaskEvent> createEventWithTraversal(String taskId,
                                                  int traverse,
                                                  boolean exec,
                                                  TaskEvent.Type type) {

    var tmp = taskId.indexOf('/');
    var objId = tmp > 0 ? taskId.substring(0, tmp):taskId;
    var subTaskId = tmp > 0 ? taskId.substring(tmp + 1):null;
    var originList = objectRepo.getOrigin(
      UUID.fromString(objId), traverse);
    return taskEventFactory.createTaskEventFromOriginList(originList, traverse, exec, subTaskId, type);
  }

  public OaasTask createTask(String taskId) {
    var context = aggregateRepo.getTaskContext(UUID.fromString(taskId));
    LOGGER.debug("createTask {}", taskId);
    return taskFactory.genTask(context, null);
  }


  public void submitCompletionEvent(String taskId) {
    TaskEvent event = new TaskEvent()
      .setId(taskId)
      .setType(TaskEvent.Type.COMPLETE);
    taskEventProcessor.processEvent(List.of(event));
  }
}
