package org.hpcclab.oaas.taskgen.service;

import io.micrometer.core.annotation.Timed;
import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.Record;
import io.vertx.mutiny.core.Vertx;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskEvent;
import org.hpcclab.oaas.model.task.TaskState;
import org.hpcclab.oaas.taskgen.TaskFactory;
import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class V2TaskEventManager {
  private static final Logger LOGGER = LoggerFactory.getLogger( V2TaskEventManager.class );

  @Inject
  TaskEventManager taskEventManager;
  @Inject
  TaskEventProcessor taskEventProcessor;
  @Inject
  @RestClient
  BlockingObjectService blockingObjectService;
  @Inject
  TaskFactory taskFactory;
  @Inject
  Vertx vertx;

  @Timed(value = "processEvent", percentiles={0.5,0.75,0.95,0.99})
  public Uni<Void> submitEventWithTraversal(String objId,
                                            int traverse,
                                            boolean exec,
                                            TaskEvent.Type type) {

    return taskEventManager
      .createEventWithTraversalUni(objId, null, traverse, exec, type)
      .flatMap(taskEvents -> {
        Uni<Void> uni = Uni.createFrom().item(() -> {
          taskEventProcessor.processEvent(taskEvents);
          return null;
        });
        return vertx.executeBlocking(uni);
      });
  }

  public List<TaskEvent> createEventWithTraversal(String taskId,
                                                  int traverse,
                                                  boolean exec,
                                                  TaskEvent.Type type) {

    var tmp = taskId.indexOf('/');
    var objId = tmp > 0 ? taskId.substring(0, tmp):taskId;
    var subTaskId = tmp > 0 ? taskId.substring(tmp + 1):null;
    return taskEventManager.createEventWithTraversal(
      objId,
      subTaskId,
      traverse,
      exec,
      type
    );
  }

  public OaasTask createTask(String taskId) {
    var context = blockingObjectService.getTaskContext(taskId);
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
