package org.hpcclab.oaas.taskmanager.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.mutiny.core.Vertx;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.hpcclab.oaas.model.proto.OaasObject;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.proto.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskEvent;
import org.hpcclab.oaas.repository.AggregateRepository;
import org.hpcclab.oaas.repository.OaasObjectRepository;
import org.hpcclab.oaas.taskmanager.factory.TaskFactory;
import org.hpcclab.oaas.taskmanager.factory.TaskEventFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class TaskEventManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(TaskEventManager.class);

  @Inject
  @Alternative
  TaskEventProcessor taskEventProcessor;
  @Inject
  AggregateRepository aggregateRepo;
  @Inject
  TaskFactory taskFactory;
  @Inject
  TaskEventFactory taskEventFactory;
  @Inject
  Vertx vertx;
  @Inject
  MeterRegistry meterRegistry;
  @Inject
  OaasObjectRepository objectRepo;

  Timer timer;

  @PostConstruct
  void setup() {
    timer = Timer.builder("submitCompletionEvent")
      .publishPercentiles(0.5, 0.75, 0.95, 0.99)
      .register(meterRegistry);
  }

  public Uni<Void> submitCreateEvent(String objId) {
    Uni<Void> uni = Uni.createFrom().item(() -> {
      LOGGER.debug("submitCreateEvent '{}'", objId);
      var event = taskEventFactory.createStartingEvent(objId);
      taskEventProcessor.processEvents(List.of(event));
      return null;
    });
    return vertx.executeBlocking(uni);
  }

  public OaasTask createTask(String taskId) {
    var context = aggregateRepo.getTaskContext(UUID.fromString(taskId));
    LOGGER.debug("createTask {}", taskId);
    return taskFactory.genTask(context);
  }


  public Uni<Void> submitCompletionEvent(List<TaskCompletion> taskCompletions) {
    var sample = Timer.start(meterRegistry);
    var events = taskCompletions.stream()
      .map(tc -> new TaskEvent().setId(tc.getId())
        .setType(TaskEvent.Type.COMPLETE)
        .setCompletion(tc))
      .toList();
    Uni<Void> uni = Uni.createFrom().item(() -> {
      taskEventProcessor.processEvents(events);
      sample.stop(timer);
      return null;
    });
    var tcMap = taskCompletions.stream()
        .collect(Collectors.toMap(tc -> UUID.fromString(tc.getId()), Function.identity()));
    return objectRepo.listAsync(tcMap.keySet())
      .flatMap(objMap -> {
        for (OaasObject obj : objMap.values()) {
          obj.setTask(tcMap.get(obj.getId()));
        }
        return objectRepo.putAllAsync(objMap);
      })
      .flatMap(ignore -> vertx.executeBlocking(uni));
  }
}
