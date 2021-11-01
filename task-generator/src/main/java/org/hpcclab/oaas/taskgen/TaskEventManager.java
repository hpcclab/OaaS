package org.hpcclab.oaas.taskgen;

import io.smallrye.reactive.messaging.kafka.Record;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hpcclab.oaas.entity.object.OaasObjectOrigin;
import org.hpcclab.oaas.entity.task.OaasTask;
import org.hpcclab.oaas.model.TaskEvent;
import org.hpcclab.oaas.taskgen.service.BlockingObjectService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class TaskEventManager {

  @Inject
  @RestClient
  BlockingObjectService originService;
  @Inject
  TaskFactory taskFactory;

  @Channel("tasks")
  Emitter<Record<String, OaasTask>> tasksEmitter;
  @Channel("task-events")
  Emitter<Record<String, TaskEvent>> taskEventEmitter;


  public void submitTask(String taskId) {
    var tmp = taskId.indexOf('/');
    var objId = tmp > 0 ? taskId.substring(0, tmp):taskId;
    var subTaskId = tmp > 0 ? taskId.substring(tmp + 1): null;
    var context = originService.getTaskContext(objId);
    OaasTask task = taskFactory.genTask(context, subTaskId);
    tasksEmitter.send(Record.of(task.getId(), task));
  }

  public void submitCompletionEvent(String taskId) {
    TaskEvent event = new TaskEvent()
      .setId(taskId)
      .setType(TaskEvent.Type.COMPLETE);
    taskEventEmitter.send(Record.of(event.getId(), event));
  }

  public void submitEventWithTraversal(String taskId,
                                       int traverse,
                                       TaskEvent.Type type) {
    var list = createEventWithTraversal(taskId, traverse, type);
    for (TaskEvent taskEvent : list) {
      taskEventEmitter.send(Record.of(taskEvent.getId(), taskEvent));
    }
  }

  public List<TaskEvent> createEventWithTraversal(String taskId,
                                                  int traverse,
                                                  TaskEvent.Type type) {
    var tmp = taskId.indexOf('/');
    var objId = tmp > 0 ? taskId.substring(0, tmp):taskId;
    var subTaskId = tmp > 0 ? taskId.substring(tmp + 1): null;
    var originList = originService.getOrigin(
      objId,
      traverse
    );
    List< TaskEvent> results = new ArrayList<>();
    for (int i = 1; i < originList.size(); i++) {
      var map = originList.get(i);
      var prevMap = originList.get(i - 1);
      for (Map.Entry<String, OaasObjectOrigin> entry : map.entrySet()) {
        var origin = entry.getValue();
        var id = UUID.fromString(entry.getKey());
        var nextTask = prevMap.entrySet().stream()
          .filter(e -> e.getValue().getParentId()==id ||
            e.getValue().getAdditionalInputs().contains(id)
          )
          .map(Map.Entry::getKey)
          .map(s -> OaasTask.createId(s, subTaskId))
          .collect(Collectors.toSet());
        var newEvent = new TaskEvent()
          .setType(type)
          .setId(entry.getKey())
          .setNextTasks(nextTask)
          .setTraverse(i==originList.size() - 1 ? traverse:0);
        if (origin.getParentId()!=null) {
          var prevTasks = Stream.concat(
              Stream.of(origin.getParentId()),
              origin.getAdditionalInputs().stream()
            )
            .map(s -> s + "/" + subTaskId)
            .collect(Collectors.toSet());
          newEvent.setPrevTasks(prevTasks);
        }
        results.add(newEvent);
      }
    }
    return results;

  }

}
