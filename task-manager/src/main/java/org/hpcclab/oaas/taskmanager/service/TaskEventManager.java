package org.hpcclab.oaas.taskmanager.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.proto.TaskCompletion;
import org.hpcclab.oaas.model.task.V2TaskEvent;
import org.hpcclab.oaas.repository.AggregateRepository;
import org.hpcclab.oaas.repository.OaasObjectRepository;
import org.hpcclab.oaas.taskmanager.factory.TaskFactory;
import org.hpcclab.oaas.taskmanager.factory.TaskEventFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TaskEventManager {
  private static final Logger LOGGER = LoggerFactory.getLogger( TaskEventManager.class );

  @Inject
  TaskEventProcessor taskEventProcessor;
  @Inject
  OaasObjectRepository objectRepo;
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

  Timer timer;

  @PostConstruct
  void setup() {
    timer = Timer.builder("submitCompletionEvent")
      .publishPercentiles(0.5,0.75,0.95,0.99)
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
    return taskFactory.genTask(context, null);
  }


  public Uni<Void> submitCompletionEvent(List<TaskCompletion> taskCompletions) {
    var sample =Timer.start(meterRegistry);
    var events = taskCompletions.stream()
      .map(tc -> new V2TaskEvent().setId(tc.getId()).setType(V2TaskEvent.Type.COMPLETE))
      .toList();
    Uni<Void> uni = Uni.createFrom().item(() -> {
      taskEventProcessor.processEvents(events);
      sample.stop(timer);
      return null;
    });
    return vertx.executeBlocking(uni);
  }
}
