package org.hpcclab.oaas.taskgen.stream;

import io.vertx.core.json.Json;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.hpcclab.oaas.model.task.BaseTaskMessage;
import org.hpcclab.oaas.model.task.TaskEvent;
import org.hpcclab.oaas.model.task.TaskState;
import org.hpcclab.oaas.taskgen.TaskEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TaskEventTransformer implements Transformer<String, TaskEvent, Iterable<KeyValue<String, BaseTaskMessage>>> {
  private static final Logger LOGGER = LoggerFactory.getLogger(TaskEventTransformer.class);
  final String storeName;
  final TaskEventManager taskEventManager;
  KeyValueStore<String, TaskState> tsStore;
  ProcessorContext context;


  public TaskEventTransformer(String storeName,
                              TaskEventManager taskEventManager) {
    this.storeName = storeName;
    this.taskEventManager = taskEventManager;
  }

  @Override
  public void init(ProcessorContext context) {
    this.context = context;
    tsStore = context.getStateStore(storeName);
  }

  @Override
  public Iterable<KeyValue<String,BaseTaskMessage>> transform(String key,
                                                                       TaskEvent taskEvent) {
    return switch (taskEvent.getType()) {
      case CREATE -> handleCreate(key, taskEvent);
      case NOTIFY -> handleNotify(key, taskEvent);
      case COMPLETE -> handleComplete(key, taskEvent);
    };
  }

  @Override
  public void close() {
  }

  private List<KeyValue<String, BaseTaskMessage>> handleCreate(String key,
                                                         TaskEvent taskEvent) {
    var taskState = tsStore.get(key);
    if (taskState==null) taskState = new TaskState();
    if (taskState.getNextTasks()==null) {
      taskState.setNextTasks(taskEvent.getNextTasks());
    } else {
      taskState.getNextTasks().addAll(taskEvent.getNextTasks());
    }

    if (taskEvent.getPrevTasks()!=null && taskState.getPrevTasks()==null) {
      taskState.setPrevTasks(taskEvent.getPrevTasks());
    }

    if (taskState.getCompletedPrevTasks() == null) {
      taskState.setCompletedPrevTasks(new HashSet<>());
    }
    taskState.getCompletedPrevTasks().addAll(taskEvent.getRoots());

    List<KeyValue<String, BaseTaskMessage>> kvList = new ArrayList<>();

    if (taskState.getPrevTasks()==null || taskState.getPrevTasks().isEmpty()) {
      kvList.addAll(notifyNext(key, taskEvent.isExec(), taskState));
    } else if (taskState.isComplete()) {
      kvList.addAll(notifyNext(key, taskEvent.isExec(), taskState));
    } else if (taskEvent.getTraverse() > 0) {
      var eventList = taskEventManager.createEventWithTraversal(
          key,
          taskEvent.getTraverse(),
          taskEvent.isExec(),
          taskEvent.getType()
        );

      if (!eventList.isEmpty()) {
        taskState.getCompletedPrevTasks()
          .addAll(eventList.get(0).getRoots());
      }

      kvList.addAll(eventList.stream()
        .skip(1)
        .map(te -> KeyValue.pair(te.getId(),(BaseTaskMessage) te))
        .toList());
    }

    if (taskEvent.isExec()) {
      if (taskState.getPrevTasks().equals(taskState.getCompletedPrevTasks()) && !taskState.isSubmitted()) {
//        taskEventManager.submitTask(key);
        kvList.add(KeyValue.pair(key,taskEventManager.createTask(key)));
        taskState.setSubmitted(true);
      }
    }

    tsStore.put(key, taskState);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("taskState {}", Json.encodePrettily(taskState));
      LOGGER.debug("Send new event {}", Json.encodePrettily(kvList));
    }
    return kvList;
  }

  private List<KeyValue<String, BaseTaskMessage>> handleNotify(String key,
                                                         TaskEvent taskEvent) {
    var taskState = tsStore.get(key);
    if (taskState==null) taskState = new TaskState();
    if (taskState.getCompletedPrevTasks()==null)
      taskState.setCompletedPrevTasks(new HashSet<>());
    taskState.getCompletedPrevTasks().add(taskEvent.getNotifyFrom());

    List<KeyValue<String, BaseTaskMessage>> kvList = List.of();

    if (taskState.isComplete()) {
      kvList = notifyNext(key, taskEvent.isExec(), taskState);
    } else if (taskState.getPrevTasks().equals(taskState.getCompletedPrevTasks()) && !taskState.isSubmitted()) {
//      taskEventManager.submitTask(key);
      kvList = List.of(KeyValue.pair(key,taskEventManager.createTask(key)));
      taskState.setSubmitted(true);
    }

    tsStore.put(key, taskState);
    return kvList;
  }

  private List<KeyValue<String, BaseTaskMessage>> handleComplete(String key,
                                                           TaskEvent taskEvent) {
    var taskState = tsStore.get(key);
    if (taskState==null) taskState = new TaskState();
    taskState.setComplete(true);
    tsStore.put(key, taskState);
    return notifyNext(key, taskEvent.isExec(), taskState);
  }

  private List<KeyValue<String, BaseTaskMessage>> notifyNext(String key,
                                                       boolean exec,
                                                       TaskState taskState) {
    if (taskState.getNextTasks() == null) {
      taskState.setNextTasks(Set.of());
      return List.of();
    }
    return taskState.getNextTasks()
      .stream()
      .<BaseTaskMessage>map(id -> new TaskEvent()
        .setId(id)
        .setType(TaskEvent.Type.NOTIFY)
        .setExec(exec)
        .setNotifyFrom(key))
      .map(te -> KeyValue.pair(te.getId(), te))
      .toList();
  }
}
