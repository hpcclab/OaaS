package org.hpcclab.oaas.taskgen.stream;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.hpcclab.oaas.model.TaskEvent;
import org.hpcclab.oaas.model.TaskState;
import org.hpcclab.oaas.taskgen.TaskEventManager;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashSet;
import java.util.List;

public class TaskEventTransformer implements Transformer<String, TaskEvent, Iterable<KeyValue<String, TaskEvent>>> {

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
  public List<KeyValue<String, TaskEvent>> transform(String key,
                                                     TaskEvent taskEvent) {
    return switch (taskEvent.getType()) {
      case CREATE -> handleCreate(key, taskEvent);
      case EXEC -> handleExec(key, taskEvent);
      case NOTIFY -> handleNotify(key, taskEvent);
      case COMPLETE -> handleComplete(key, taskEvent);
    };
  }

  @Override
  public void close() {
  }

  private List<KeyValue<String, TaskEvent>> handleExec(String key,
                                                       TaskEvent taskEvent) {

    var list = handleCreate(key, taskEvent);
    if (list.isEmpty()) {
      var taskState = tsStore.get(key);
      if (taskState.getPrevTasks().isEmpty() &&
        !taskState.isSubmitted()) {
        taskEventManager.submitTask(key);
        taskState.setSubmitted(true);
      }
      tsStore.put(key, taskState);
    }
    return list;
  }

  private List<KeyValue<String, TaskEvent>> handleCreate(String key,
                                                         TaskEvent taskEvent) {
    var taskState = tsStore.get(key);
    if (taskState==null) taskState = new TaskState();
    if (taskState.getNextTasks()==null) {
      taskState.setNextTasks(taskEvent.getNextTasks());
    } else {
      taskState.getNextTasks().addAll(taskEvent.getNextTasks());
    }

    if (taskEvent.getPrevTasks()!=null &&
      taskState.getPrevTasks()==null) {
      taskState.setPrevTasks(taskEvent.getPrevTasks());
    }

    List<KeyValue<String, TaskEvent>> kvList = List.of();

    if (taskState.isComplete()) {
      kvList = notifyNext(key, taskState);
    } else if (taskEvent.getTraverse() > 0) {
      kvList = taskEventManager.createEventWithTraversal(
          key,
          taskEvent.getTraverse(),
          taskEvent.getType()
        )
        .stream().map(te -> KeyValue.pair(te.getId(), te))
        .toList();
    }

    tsStore.put(key, taskState);
    return kvList;
  }

  private List<KeyValue<String, TaskEvent>> handleNotify(String key,
                                                         TaskEvent taskEvent) {
    var taskState = tsStore.get(key);
    if (taskState.getCompletedPrevTasks()==null)
      taskState.setCompletedPrevTasks(new HashSet<>());
    taskState.getCompletedPrevTasks().add(taskEvent.getNotifyFrom());

    List<KeyValue<String, TaskEvent>> kvList = List.of();

    if (taskState.isComplete()) {
      kvList = notifyNext(key, taskState);
    } else if (taskState.getPrevTasks().equals(taskState.getCompletedPrevTasks())) {
      taskEventManager.submitTask(key);
      taskState.setSubmitted(true);
    }

    tsStore.put(key, taskState);
    return kvList;
  }

  private List<KeyValue<String, TaskEvent>> handleComplete(String key,
                                                           TaskEvent taskEvent) {
    var taskState = tsStore.get(key);
    taskState.setComplete(true);
    tsStore.put(key, taskState);
    return notifyNext(key, taskState);
  }

  private List<KeyValue<String, TaskEvent>> notifyNext(String key,
                                                       TaskState taskState) {
    return taskState.getNextTasks()
      .stream()
      .map(id -> new TaskEvent()
        .setId(id)
        .setType(TaskEvent.Type.NOTIFY)
        .setNotifyFrom(key))
      .map(te -> KeyValue.pair(te.getId(), te))
      .toList();
  }
}
