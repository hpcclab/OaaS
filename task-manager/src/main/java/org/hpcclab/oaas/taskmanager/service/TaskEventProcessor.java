package org.hpcclab.oaas.taskmanager.service;

import io.micrometer.core.annotation.Timed;
import io.vertx.core.json.Json;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hpcclab.oaas.model.task.TaskState;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.hpcclab.oaas.model.task.TaskEvent;
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
  public void processEvents(List<TaskEvent> taskEvents) {
    for (TaskEvent taskEvent : taskEvents) {
      handleWithRecursive(taskEvent);
    }
  }

  private List<TaskEvent> handleWithRecursive(TaskEvent taskEvent) {
    try {
      List<TaskEvent> nextEvents = handle(taskEvent);
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
  List<TaskEvent> handle(TaskEvent taskEvent) {
    if (LOGGER.isDebugEnabled()) LOGGER.debug("handle events {}", Json.encodePrettily(taskEvent));
    return switch (taskEvent.getType()) {
      case CREATE -> handleCreate(taskEvent);
      case NOTIFY -> handleNotify(taskEvent);
      case COMPLETE -> handleComplete(taskEvent);
    };
  }


  private List<TaskEvent> handleCreate(TaskEvent taskEvent) {
    if (taskEvent.getPrqTasks()==null || taskEvent.getPrqTasks().isEmpty()) {
      if (taskEvent.getSource() != null)
        return List.of(new TaskEvent().setType(TaskEvent.Type.NOTIFY).setId(taskEvent.getSource()).setSource(taskEvent.getId()).setExec(taskEvent.isExec()));
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

    if (taskState.isCompleted()) {
      return List.of(new TaskEvent().setType(TaskEvent.Type.NOTIFY).setId(taskEvent.getSource()).setSource(taskEvent.getId()).setExec(taskEvent.isExec()));
    }

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
    var eventList = taskEventFactory.createPrqEvent(taskEvent, TaskEvent.Type.CREATE, roots);
    taskState.getCompletedPrqTasks().addAll(roots);

    var submitting = taskEvent.isExec() && taskState.getPrqTasks().equals(taskState.getCompletedPrqTasks()) && !taskState.isSubmitted();
    if (taskState.isSubmitted() || submitting)
      taskState.setStatus(TaskStatus.DOING);

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

  private List<TaskEvent> notifyNext(TaskEvent currentEvent, TaskState taskState) {
    var next = currentEvent.getNextTasks();
    if (next==null) next = Set.of();
    if (taskState.getNextTasks()!=null && !taskState.getNextTasks().isEmpty()) {
      next = new HashSet<>(next);
      next.addAll(taskState.getNextTasks());
    }
    if (next.isEmpty()) return List.of();
    return next.stream().map(id -> new TaskEvent().setId(id).setType(TaskEvent.Type.NOTIFY).setExec(currentEvent.isExec()).setSource(currentEvent.getId())).toList();
  }

  private List<TaskEvent> handleNotify(TaskEvent taskEvent) {
    var taskStateMetadata = taskStateRepo.getRemoteCache().getWithMetadata(taskEvent.getId());
    var version = taskStateMetadata==null ? null:taskStateMetadata.getVersion();
    var taskState = taskStateMetadata==null ? new TaskState():taskStateMetadata.getValue();

    if (taskState.isSubmitted())
      return List.of();

    if (taskState.isCompleted())
      return notifyNext(taskEvent, taskState);


    if (taskState.getCompletedPrqTasks()==null) taskState.setCompletedPrqTasks(new HashSet<>());
    taskState.getCompletedPrqTasks().add(taskEvent.getSource());

    var submitting = taskEvent.isExec() && taskState.getPrqTasks().equals(taskState.getCompletedPrqTasks()) && !taskState.isSubmitted();
    if (taskState.isSubmitted() || submitting)
      taskState.setStatus(TaskStatus.DOING);

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

  private List<TaskEvent> handleComplete(TaskEvent taskEvent) {
    var taskStateMetadata = taskStateRepo.getRemoteCache().getWithMetadata(taskEvent.getId());
    var version = taskStateMetadata==null ? null:taskStateMetadata.getVersion();
    var taskState = taskStateMetadata==null ? new TaskState():taskStateMetadata.getValue();

    if (taskState.isCompleted()) {
      return List.of();
    }
    taskState.update(taskEvent.getCompletion());

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
    LOGGER.debug("Submitting task {}", id);
    taskBrokerService.submitTask(id, task.getFunction().getName(),
//      task.getFunction().getProvision().getType().toString(),
      task);
  }
}
