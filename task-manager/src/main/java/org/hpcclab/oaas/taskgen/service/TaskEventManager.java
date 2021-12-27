package org.hpcclab.oaas.taskgen.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.Record;
import io.vertx.core.json.Json;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hpcclab.oaas.iface.service.ObjectService;
import org.hpcclab.oaas.model.object.OaasObjectOrigin;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskEvent;
import org.hpcclab.oaas.taskgen.TaskFactory;
import org.hpcclab.oaas.taskgen.TaskManagerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class TaskEventManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(TaskEventManager.class);

  @Inject
  @RestClient
  BlockingObjectService blockingObjectService;
  @Inject
  ObjectService objectService;
  @Inject
  TaskFactory taskFactory;
  @Inject
  TaskManagerConfig config;

  @Channel("tasks")
  Emitter<Record<String, OaasTask>> tasksEmitter;
  @Channel("task-events")
  MutinyEmitter<Record<String, TaskEvent>> taskEventEmitter;


  public void submitTask(String taskId) {
    OaasTask task = createTask(taskId);
    LOGGER.debug("submitTask {}", taskId);
    tasksEmitter.send(Record.of(task.getId(), task));
  }

  public OaasTask createTask(String taskId) {
    var tmp = taskId.indexOf('/');
    var objId = tmp > 0 ? taskId.substring(0, tmp):taskId;
    var subTaskId = tmp > 0 ? taskId.substring(tmp + 1):null;
    LOGGER.debug("getTaskContext {}", taskId);
    var context = blockingObjectService.getTaskContext(objId);
    return taskFactory.genTask(context, subTaskId);
  }

  public Uni<Void> submitCompletionEvent(String taskId) {
    TaskEvent event = new TaskEvent()
      .setId(taskId)
      .setType(TaskEvent.Type.COMPLETE);
    return taskEventEmitter.send(Record.of(event.getId(), event));
  }


  public Uni<Void> submitExecEvent(String taskId, List<Map<String, OaasObjectOrigin>> originList) {
    var tmp = taskId.indexOf('/');
    var subTaskId = tmp > 0 ? taskId.substring(tmp + 1):null;
    var list = createTaskEventFromOriginList(
      originList, config.defaultTraverse(), true, subTaskId, TaskEvent.Type.CREATE);
    return Multi.createFrom().iterable(list)
//      .log("submitExecEvent "+ taskId)
      .onItem().transformToUniAndConcatenate(taskEvent -> taskEventEmitter
        .send(Record.of(taskEvent.getId(), taskEvent)))
      .collect().last();
  }

  public Uni<Void> submitEventWithTraversal(String objId,
                                            String subTaskId,
                                            int traverse,
                                            boolean exec,
                                            TaskEvent.Type type) {
    Uni<List<TaskEvent>> uni;
    if (subTaskId==null) {
      uni = createEventWithTraversalUni(objId, subTaskId, traverse, exec, type);
    } else {
      uni = objectService.get(objId)
        .flatMap(obj -> {
          var tmp = subTaskId;
          if (obj.getState().getType()!=OaasObjectState.StateType.SEGMENTABLE)
            tmp = null;
          return createEventWithTraversalUni(objId, tmp, traverse, exec, type);
        });
    }
    return uni
      .onItem().transformToMulti(list -> Multi.createFrom().iterable(list))
      .onItem().transformToUniAndConcatenate(taskEvent ->
        taskEventEmitter.send(Record.of(taskEvent.getId(), taskEvent)))
      .collect().last();
  }


  public List<TaskEvent> createEventWithTraversal(String taskId,
                                                  int traverse,
                                                  boolean exec,
                                                  TaskEvent.Type type) {

    var tmp = taskId.indexOf('/');
    var objId = tmp > 0 ? taskId.substring(0, tmp):taskId;
    var subTaskId = tmp > 0 ? taskId.substring(tmp + 1):null;
    return createEventWithTraversal(
      objId,
      subTaskId,
      traverse,
      exec,
      type
    );
  }


  public Uni<List<TaskEvent>> createEventWithTraversalUni(String objId,
                                                          String subTaskId,
                                                          int traverse,
                                                          boolean exec,
                                                          TaskEvent.Type type) {
    return objectService.getOrigin(
        objId,
        traverse
      )
      .map(originList -> createTaskEventFromOriginList(
        originList, traverse, exec, subTaskId, type));
  }

  public List<TaskEvent> createEventWithTraversal(String objId,
                                                  String subTaskId,
                                                  int traverse,
                                                  boolean exec,
                                                  TaskEvent.Type type) {
    var originList = blockingObjectService.getOrigin(
      objId,
      traverse
    );
    return createTaskEventFromOriginList(originList, traverse, exec, subTaskId, type);
  }

  public List<TaskEvent> createTaskEventFromOriginList(List<Map<String, OaasObjectOrigin>> originList,
                                                       int traverse,
                                                       boolean exec,
                                                       String subTaskId,
                                                       TaskEvent.Type type) {
    List<TaskEvent> results = new ArrayList<>();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("createTaskEventFromOriginList originList={}", Json.encodePrettily(originList));
    }
    for (int i = 0; i < originList.size(); i++) {
      var map = originList.get(i);
      Map<String, OaasObjectOrigin> nextMap = i > 0 ? originList.get(i - 1):Map.of();
      Map<String, OaasObjectOrigin> prevMap = i + 1 < originList.size() ? originList.get(i + 1):Map.of();
      for (Map.Entry<String, OaasObjectOrigin> entry : map.entrySet()) {
        var origin = entry.getValue();
        var id = UUID.fromString(entry.getKey());

        if (origin.getParentId()==null) {
          continue;
        }

        Set<String> prevTasks = Stream.concat(
            Stream.of(origin.getParentId()),
            origin.getAdditionalInputs().stream()
          )
          .map(uuid -> OaasTask.createId(uuid.toString(), subTaskId))
          .collect(Collectors.toSet());

        Set<String> nextTasks = Set.of();

        if (!nextMap.isEmpty()) {
          nextTasks = nextMap.entrySet().stream()
            .filter(e -> e.getValue().getParentId().equals(id) ||
              e.getValue().getAdditionalInputs().contains(id)
            )
            .map(Map.Entry::getKey)
            .map(s -> OaasTask.createId(s, subTaskId))
            .collect(Collectors.toSet());
        }

        var roots = prevMap.entrySet().stream()
          .filter(e -> e.getValue().getParentId()==null)
          .filter(e -> e.getKey().equals(origin.getParentId().toString())
            || origin.getAdditionalInputs().contains(UUID.fromString(e.getKey())))
          .map(Map.Entry::getKey)
          .map(s -> OaasTask.createId(s, subTaskId))
          .collect(Collectors.toSet());

        var newEvent = new TaskEvent()
          .setType(type)
          .setId(OaasTask.createId(entry.getKey(), subTaskId))
          .setNextTasks(nextTasks)
          .setPrevTasks(prevTasks)
          .setRoots(roots)
          .setExec(exec)
          .setTraverse(i==originList.size() - 1 ? traverse:0);

        results.add(newEvent);
      }
    }
    return results;
  }

}
