package org.hpcclab.oaas.taskmanager.service;

import io.micrometer.core.annotation.Timed;
import io.vertx.core.json.Json;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hpcclab.oaas.model.proto.TaskState;
import org.hpcclab.oaas.model.task.V2TaskEvent;
import org.hpcclab.oaas.repository.TaskStateRepository;
import org.hpcclab.oaas.taskmanager.TaskEventException;
import org.hpcclab.oaas.taskmanager.factory.TaskEventFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class TaskEventProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(TaskEventProcessor.class);
  @Inject
  TaskStateRepository taskStateRepo;
  @Inject
  TaskEventFactory taskEventFactory;
  @Inject
  TaskEventManager taskEventManager;
  @RestClient
  TaskBrokerService taskBrokerService;

  @Timed(value = "processEvents", percentiles = {0.5, 0.75, 0.95, 0.99})
  public void processEvents(List<V2TaskEvent> taskEvents) {
    for (V2TaskEvent taskEvent : taskEvents) {
      handleWithRecursive(taskEvent);
    }
  }

  private List<V2TaskEvent> handleWithRecursive(V2TaskEvent taskEvent) {
    try {
      List<V2TaskEvent> nextEvents = handle(taskEvent);
      if (nextEvents.isEmpty()) {
        return nextEvents;
      } else {
        return nextEvents.stream().flatMap(te -> handleWithRecursive(te).stream())
          .toList();
      }
    } catch (TaskEventException taskEventException) {
      return List.of();
    }
  }

  @Retry(maxRetries = 3, maxDuration = 3000, retryOn = TaskEventException.class)
  List<V2TaskEvent> handle(V2TaskEvent taskEvent) {
    if (LOGGER.isDebugEnabled()) LOGGER.debug("handle events {}", Json.encodePrettily(taskEvent));
    return switch (taskEvent.getType()) {
      case CREATE -> handleCreate(taskEvent);
      case NOTIFY -> handleNotify(taskEvent);
      case COMPLETE -> handleComplete(taskEvent);
    };
  }


  private List<V2TaskEvent> handleCreate(V2TaskEvent taskEvent) {
    if (taskEvent.getPrqTasks()==null || taskEvent.getPrqTasks().isEmpty()) {
      if (taskEvent.getSource() != null)
        return List.of(new V2TaskEvent().setType(V2TaskEvent.Type.NOTIFY).setId(taskEvent.getSource()).setSource(taskEvent.getId()).setExec(taskEvent.isExec()));
      else
        return List.of();
    }

    var remoteCache = taskStateRepo.getRemoteCache();

    var taskStateMetadata = remoteCache.getWithMetadata(taskEvent.getId());
    var version = taskStateMetadata==null ? null:taskStateMetadata.getVersion();
    var taskState = taskStateMetadata==null ? new TaskState():taskStateMetadata.getValue();

    if (taskEvent.isEntry() && taskStateMetadata != null) {
      return List.of();
    }

    // 1 check completion
    if (taskState.isComplete()) {
      return List.of(new V2TaskEvent().setType(V2TaskEvent.Type.NOTIFY).setId(taskEvent.getSource()).setSource(taskEvent.getId()).setExec(taskEvent.isExec()));
    }

    // 3 set task state parameter
    if (taskState.getNextTasks()==null) {
      taskState.setNextTasks(taskEvent.getNextTasks());
    } else if (taskEvent.getNextTasks()!=null) {
      taskState.getNextTasks().addAll(taskEvent.getNextTasks());
    }

    if (taskEvent.getPrqTasks()!=null && taskState.getPrqTasks()==null) {
      taskState.setPrqTasks(taskEvent.getPrqTasks());
    }

    if (taskState.getCompletedPrqTasks()==null) {
      taskState.setCompletedPrqTasks(new HashSet<>());
    }

    var roots = new ArrayList<String>();
    var eventList = taskEventFactory.createPrqEvent(taskEvent, V2TaskEvent.Type.CREATE, roots);
    taskState.getCompletedPrqTasks().addAll(roots);

    var submitting = taskEvent.isExec() && taskState.getPrqTasks().equals(taskState.getCompletedPrqTasks()) && !taskState.isSubmitted();
    taskState.setSubmitted(taskState.isSubmitted() || submitting);

    boolean success;
    if (version!=null) {
      success = remoteCache.replaceWithVersion(taskEvent.getId(), taskState, version);
    } else {
      success = taskStateRepo.getRemoteCache().putIfAbsent(taskEvent.getId(), taskState)==null;
    }

    LOGGER.debug("processing CREATE event id={} success={} submitting={}", taskEvent.getId(), success, submitting);

    if (!success) {
      throw TaskEventException.concurrentModification();
    }
    if (submitting) {
      submitTask(taskEvent.getId());
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("taskState {}", Json.encodePrettily(taskState));
    }

    return eventList;
  }

  private List<V2TaskEvent> notifyNext(V2TaskEvent currentEvent, TaskState taskState) {
    var next = currentEvent.getNextTasks();
    if (next==null) next = Set.of();
    if (taskState.getNextTasks()!=null && !taskState.getNextTasks().isEmpty()) {
      next = new HashSet<>(next);
      next.addAll(taskState.getNextTasks());
    }
    if (next.isEmpty()) return List.of();
    return next.stream().map(id -> new V2TaskEvent().setId(id).setType(V2TaskEvent.Type.NOTIFY).setExec(currentEvent.isExec()).setSource(currentEvent.getId())).toList();
  }

  private List<V2TaskEvent> handleNotify(V2TaskEvent taskEvent) {
    var taskStateMetadata = taskStateRepo.getRemoteCache().getWithMetadata(taskEvent.getId());
    var version = taskStateMetadata==null ? null:taskStateMetadata.getVersion();
    var taskState = taskStateMetadata==null ? new TaskState():taskStateMetadata.getValue();
    if (taskState.isSubmitted()) return List.of();

    if (taskState.isComplete()) {
      return notifyNext(taskEvent, taskState);
    }

    if (taskState.getCompletedPrqTasks()==null) taskState.setCompletedPrqTasks(new HashSet<>());
    taskState.getCompletedPrqTasks().add(taskEvent.getSource());

    var submitting = taskEvent.isExec() && taskState.getPrqTasks().equals(taskState.getCompletedPrqTasks()) && !taskState.isSubmitted();
    taskState.setSubmitted(taskState.isSubmitted() || submitting);

    var success = false;
    if (version!=null) {
      success = taskStateRepo.getRemoteCache().replaceWithVersion(taskEvent.getId(), taskState, version);
    } else {
      success = taskStateRepo.getRemoteCache().putIfAbsent(taskEvent.getId(), taskState)==null;
    }

    LOGGER.debug("processing NOTIFY event id={} success={} submitting={}", taskEvent.getId(), success, submitting);
    if (!success) {
      throw TaskEventException.concurrentModification();
    }
    if (submitting) {
      submitTask(taskEvent.getId());
    }
    return List.of();
  }

  private List<V2TaskEvent> handleComplete(V2TaskEvent taskEvent) {
    var taskStateMetadata = taskStateRepo.getRemoteCache().getWithMetadata(taskEvent.getId());
    var version = taskStateMetadata==null ? null:taskStateMetadata.getVersion();
    var taskState = taskStateMetadata==null ? new TaskState():taskStateMetadata.getValue();

    if (taskState.isComplete()) {
      return List.of();
    }
    taskState.setComplete(true);

    var success = false;
    if (version!=null) {
      success = taskStateRepo.getRemoteCache().replaceWithVersion(taskEvent.getId(), taskState, version);
    } else {
      success = taskStateRepo.getRemoteCache().putIfAbsent(taskEvent.getId(), taskState)==null;
    }

    if (!success) {
      throw TaskEventException.concurrentModification();
    }

    return notifyNext(taskEvent, taskState);
  }

  public void submitTask(String id) {
    var task = taskEventManager.createTask(id);
    taskBrokerService.submitTask(id, task.getFunction().getName(), task.getFunction().getProvision().getType().toString(), task);
  }
}
