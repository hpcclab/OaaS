package org.hpcclab.oaas.taskmanager.service;

import io.micrometer.core.annotation.Timed;
import io.vertx.core.json.Json;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hpcclab.oaas.model.proto.TaskState;
import org.hpcclab.oaas.model.task.V2TaskEvent;
import org.hpcclab.oaas.repository.TaskStateRepository;
import org.hpcclab.oaas.taskmanager.TaskEventException;
import org.hpcclab.oaas.taskmanager.factory.TaskEventFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class V2TaskEventProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(V2TaskEventProcessor.class);
  final int maxRetries = 3;
  @Inject
  TaskStateRepository taskStateRepo;
  @Inject
  TaskEventFactory taskEventFactory;
  @Inject
  TaskEventManager taskEventManager;
  @RestClient
  TaskBrokerService taskBrokerService;
  TransactionManager transactionManager;

  @PostConstruct
  void setup() {
    this.transactionManager = taskStateRepo.getRemoteCache().getTransactionManager();
  }

  @Timed(value = "processEventsV2", percentiles = {0.5, 0.75, 0.95, 0.99})
  public void processEvents(List<V2TaskEvent> taskEvents) {
    for (V2TaskEvent taskEvent : taskEvents) {
      handle(taskEvent);
    }
  }

  private List<V2TaskEvent> handle(V2TaskEvent taskEvent) {
    if (LOGGER.isDebugEnabled())
      LOGGER.debug("handle events {}", Json.encodePrettily(taskEvent));
    try {
      var list = switch (taskEvent.getType()) {
        case CREATE -> handleCreate(taskEvent);
        case NOTIFY -> handleNotify(taskEvent);
        case COMPLETE -> handleComplete(taskEvent);
      };

      if (list.isEmpty()) {
        return list;
      } else {
        return list.stream()
          .flatMap(te -> handle(te).stream())
          .toList();
      }
    } catch (SystemException e) {
      throw new TaskEventException(e);
    }
  }


  private List<V2TaskEvent> handleCreate(V2TaskEvent taskEvent) throws SystemException {
    if (taskEvent.getPrqTasks()==null || taskEvent.getPrqTasks().isEmpty()) {
      return List.of(
        new V2TaskEvent()
          .setType(V2TaskEvent.Type.NOTIFY)
          .setId(taskEvent.getSource())
          .setSource(taskEvent.getId())
          .setExec(taskEvent.isExec())
      );
    }
    List<V2TaskEvent> eventList = List.of();

    try {
      transactionManager.begin();
      var taskState = taskStateRepo.get(taskEvent.getId());
      if (taskState == null) taskState = new TaskState();

      if (taskState.isComplete()) {
        transactionManager.commit();
        if (taskEvent.getSource() != null)
          return List.of(
            new V2TaskEvent()
              .setType(V2TaskEvent.Type.NOTIFY)
              .setId(taskEvent.getSource())
              .setSource(taskEvent.getId())
              .setExec(taskEvent.isExec())
          );
        else
          return List.of();
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
      eventList = taskEventFactory
        .createPrqEvent(taskEvent, V2TaskEvent.Type.CREATE, roots);
      taskState.getCompletedPrqTasks().addAll(roots);

      var submitting = taskEvent.isExec()
        && taskState.getPrqTasks().equals(taskState.getCompletedPrqTasks())
        && !taskState.isSubmitted();
      taskState.setSubmitted(taskState.isSubmitted() || submitting);

      taskStateRepo.put(taskEvent.getId(), taskState);

      if (submitting) {
        submitTask(taskEvent.getId());
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("taskState {}", Json.encodePrettily(taskState));
      }
      transactionManager.commit();
    } catch (Exception e) {
      transactionManager.rollback();
      throw new TaskEventException(e);
    }

    return eventList;
  }

  private List<V2TaskEvent> notifyNext(V2TaskEvent currentEvent,
                                       TaskState taskState) {
    var next = currentEvent.getNextTasks();
    if (next==null) next = Set.of();
    if (taskState.getNextTasks()!=null && !taskState.getNextTasks().isEmpty()) {
      next = new HashSet<>(next);
      next.addAll(taskState.getNextTasks());
    }
    if (next.isEmpty()) return List.of();
    return next
      .stream()
      .map(id -> new V2TaskEvent()
        .setId(id)
        .setType(V2TaskEvent.Type.NOTIFY)
        .setExec(currentEvent.isExec())
        .setSource(currentEvent.getId())
      )
      .toList();
  }

  private List<V2TaskEvent> handleNotify(V2TaskEvent taskEvent) throws SystemException {
//    try {
//      transactionManager.begin();
      var taskState = taskStateRepo.get(taskEvent.getId());
      if (taskState == null) taskState = new TaskState();

      if (taskState.isSubmitted()) {
//        transactionManager.commit();
        return List.of();
      }

      if (taskState.isComplete()) {
//        transactionManager.commit();
        return notifyNext(taskEvent, taskState);
      }

      if (taskState.getCompletedPrqTasks()==null)
        taskState.setCompletedPrqTasks(new HashSet<>());
      taskState.getCompletedPrqTasks().add(taskEvent.getSource());

      var submitting = taskEvent.isExec()
        && taskState.getPrqTasks().equals(taskState.getCompletedPrqTasks())
        && !taskState.isSubmitted();
      taskState.setSubmitted(taskState.isSubmitted() || submitting);

      taskStateRepo.put(taskEvent.getId(), taskState);

      if (submitting) {
        submitTask(taskEvent.getId());
      }
//      transactionManager.commit();
//    } catch (Exception e) {
//      transactionManager.rollback();
//    }
    return List.of();
  }

  private List<V2TaskEvent> handleComplete(V2TaskEvent taskEvent) throws SystemException {
    List<V2TaskEvent> events = List.of();
//    try {
//      transactionManager.begin();
      var taskState = taskStateRepo.get(taskEvent.getId());
      if (taskState == null) taskState = new TaskState();

      if (taskState.isComplete()) {
//        transactionManager.commit();
        return List.of();
      }
      taskState.setComplete(true);

      taskStateRepo.put(taskEvent.getId(), taskState);
      events = notifyNext(taskEvent, taskState);
//      transactionManager.commit();
//    } catch (Exception e) {
//      transactionManager.rollback();
//    }
    return events;
  }

  public void submitTask(String id) {
    var task = taskEventManager.createTask(id);
    taskBrokerService.submitTask(id,
      task.getFunction().getName(),
      task.getFunction().getProvision().getType().toString(),
      task
    );
  }
}
