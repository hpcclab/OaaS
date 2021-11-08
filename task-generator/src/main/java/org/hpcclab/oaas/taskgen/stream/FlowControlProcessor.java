package org.hpcclab.oaas.taskgen.stream;

import io.vertx.core.json.Json;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.hpcclab.oaas.model.task.BaseTaskMessage;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskEvent;
import org.hpcclab.oaas.model.task.TaskState;
import org.hpcclab.oaas.taskgen.TaskEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FlowControlProcessor implements Processor
  <String, TaskEvent, String, BaseTaskMessage> {
  private static final Logger LOGGER = LoggerFactory.getLogger(FlowControlProcessor.class);

  final String storeName;
  final TaskEventManager taskEventManager;
  final boolean enableCloudEventHeader;
  KeyValueStore<String, TaskState> tsStore;
  ProcessorContext<String, BaseTaskMessage> context;

  FlowControlProcessor(String storeName,
                       TaskEventManager taskEventManager,
                       boolean enableCloudEventHeader) {
    this.storeName = storeName;
    this.taskEventManager = taskEventManager;
    this.enableCloudEventHeader = enableCloudEventHeader;
  }

  @Override
  public void init(ProcessorContext<String, BaseTaskMessage> context) {
    this.context = context;
    tsStore = context.getStateStore(storeName);

  }

  @Override
  public void process(Record<String, TaskEvent> record) {
    switch (record.value().getType()) {
      case CREATE -> handleCreate(record);
      case NOTIFY -> handleNotify(record);
      case COMPLETE -> handleComplete(record);
    }
  }

  private void handleCreate(Record<String, TaskEvent> record) {
    String key = record.key();
    var taskEvent = record.value();
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

    if (taskState.getCompletedPrevTasks()==null) {
      taskState.setCompletedPrevTasks(new HashSet<>());
    }
    taskState.getCompletedPrevTasks().addAll(taskEvent.getRoots());

    List<KeyValue<String, BaseTaskMessage>> kvList = new ArrayList<>();

    if (taskState.getPrevTasks()==null || taskState.getPrevTasks().isEmpty()) {
      notifyNext(key, taskEvent.isExec(), taskState);
    } else if (taskState.isComplete()) {
      notifyNext(key, taskEvent.isExec(), taskState);
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

      eventList.stream()
        .skip(1)
        .forEach(this::forward);
    }

    if (taskEvent.isExec()) {
      if (taskState.getPrevTasks().equals(taskState.getCompletedPrevTasks()) && !taskState.isSubmitted()) {
//        kvList.add(KeyValue.pair(key, taskEventManager.createTask(key)));
        forward(taskEventManager.createTask(key));
        taskState.setSubmitted(true);
      }
    }


    tsStore.put(key, taskState);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("taskState {}", Json.encodePrettily(taskState));
      LOGGER.debug("Send new event {}", Json.encodePrettily(kvList));
    }
  }


  private void handleNotify(Record<String, TaskEvent> record) {
    String key = record.key();
    var taskEvent = record.value();
    var taskState = tsStore.get(key);
    if (taskState==null) taskState = new TaskState();
    if (taskState.getCompletedPrevTasks()==null)
      taskState.setCompletedPrevTasks(new HashSet<>());
    taskState.getCompletedPrevTasks().add(taskEvent.getNotifyFrom());
    if (taskState.isComplete()) {
      notifyNext(key, taskEvent.isExec(), taskState);
    } else if (taskState.getPrevTasks().equals(taskState.getCompletedPrevTasks())
      && !taskState.isSubmitted()) {
      forward(taskEventManager.createTask(key));
      taskState.setSubmitted(true);
    }

    tsStore.put(key, taskState);
  }

  private void handleComplete(Record<String, TaskEvent> record) {
    String key = record.key();
    var taskEvent = record.value();
    var taskState = tsStore.get(key);
    if (taskState==null) taskState = new TaskState();
    taskState.setComplete(true);
    tsStore.put(key, taskState);
    notifyNext(key, taskEvent.isExec(), taskState);
  }


  private void notifyNext(String key,
                          boolean exec,
                          TaskState taskState) {
    if (taskState.getNextTasks()==null) {
      taskState.setNextTasks(Set.of());
      return;
    }
    taskState.getNextTasks()
      .stream()
      .map(id -> new TaskEvent()
        .setId(id)
        .setType(TaskEvent.Type.NOTIFY)
        .setExec(exec)
        .setNotifyFrom(key))
      .forEach(this::forward);
  }

  public Record<String, BaseTaskMessage> makeRecord(TaskEvent te) {
    return new Record<>(te.getId(), te, System.currentTimeMillis());
  }

  public Record<String, BaseTaskMessage> makeRecord(OaasTask task) {
    RecordHeaders recordHeaders = new RecordHeaders();
    recordHeaders
      .add("content-type", "application/json".getBytes())
      .add("ce-id", task.getId().getBytes())
      .add("ce-specversion", "1.0".getBytes())
      .add("ce-source", "oaas/task-generator".getBytes())
      .add("ce-function", task.getFunction().getName().getBytes())
      .add("ce-type", "oaas.task".getBytes());
    return new Record<>(task.getId(), task, System.currentTimeMillis(),
      recordHeaders);
  }

  public void forward(TaskEvent taskEvent) {
    context.forward(makeRecord(taskEvent), "task-events");
  }

  public void forward(OaasTask oaasTask) {
    context.forward(makeRecord(oaasTask), "tasks");
  }
}
