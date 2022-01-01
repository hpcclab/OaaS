package org.hpcclab.oaas.taskmanager.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskEvent;
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
  V2TaskEventProcessor v2TaskEventProcessor;
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

  Timer timer1;
  Timer timer2;

  @PostConstruct
  void setup() {
    timer1 = Timer.builder("submitEventWithTraversal")
      .publishPercentiles(0.5,0.75,0.95,0.99)
      .register(meterRegistry);
    timer2 = Timer.builder("submitCompletionEvent")
      .publishPercentiles(0.5,0.75,0.95,0.99)
      .register(meterRegistry);
  }

  public Uni<Void> submitEventWithTraversal(String objId,
                                            int traverse,
                                            boolean exec,
                                            TaskEvent.Type type) {

    var sample =Timer.start(meterRegistry);
    Uni<Void> uni = Uni.createFrom().item(() -> {
      var events = createEventWithTraversal(objId, traverse, exec,type);
      taskEventProcessor.processEvents(events);
      sample.stop(timer1);
      return null;
    });
    return vertx.executeBlocking(uni);
  }

  public Uni<Void> submitCreateEvent(String objId) {
    Uni<Void> uni = Uni.createFrom().item(() -> {
      var event = taskEventFactory.createStartingEvent(objId);
      v2TaskEventProcessor.processEvents(List.of(event));
      return null;
    });
    return vertx.executeBlocking(uni);
  }

//  public Uni<Void> processEvents(Supplier<List<TaskEvent>> eventsSupplier) {
//    var start = System.currentTimeMillis();
//    Uni<Void> uni = Uni.createFrom().item(() -> {
//      var e = eventsSupplier.get();
//      taskEventProcessor.processEvent(e);
//      LOGGER.info("Processed {} events in {} ms", e.size(), System.currentTimeMillis() - start);
//      return null;
//    });
//    return vertx.executeBlocking(uni);
//  }

  public List<TaskEvent> createEventWithTraversal(String taskId,
                                                  int traverse,
                                                  boolean exec,
                                                  TaskEvent.Type type) {

    var tmp = taskId.indexOf('/');
    var objId = tmp > 0 ? taskId.substring(0, tmp):taskId;
    var subTaskId = tmp > 0 ? taskId.substring(tmp + 1):null;
    var originList = objectRepo.getOrigin(
      UUID.fromString(objId), traverse);
    return taskEventFactory.createTaskEventFromOriginList(originList, traverse, exec, subTaskId, type);
  }

  public OaasTask createTask(String taskId) {
    var context = aggregateRepo.getTaskContext(UUID.fromString(taskId));
    LOGGER.debug("createTask {}", taskId);
    return taskFactory.genTask(context, null);
  }


  public Uni<Void> submitCompletionEvent(List<TaskCompletion> taskCompletions) {
    var sample =Timer.start(meterRegistry);
//    var events = taskCompletions.stream()
//      .map(tc -> new TaskEvent().setId(tc.getId()).setType(TaskEvent.Type.COMPLETE))
//      .toList();
//    Uni<Void> uni = Uni.createFrom().item(() -> {
//      taskEventProcessor.processEvents(events);
//      sample.stop(timer2);
//      return null;
//    });
    var events = taskCompletions.stream()
      .map(tc -> new V2TaskEvent().setId(tc.getId()).setType(V2TaskEvent.Type.COMPLETE))
      .toList();
    Uni<Void> uni = Uni.createFrom().item(() -> {
      v2TaskEventProcessor.processEvents(events);
      sample.stop(timer2);
      return null;
    });
    return vertx.executeBlocking(uni);
  }

  public void submitCompletionEventBlocking(List<TaskCompletion> taskCompletions) {
    var sample =Timer.start(meterRegistry);
    var events = taskCompletions.stream()
      .map(tc -> new TaskEvent().setId(tc.getId()).setType(TaskEvent.Type.COMPLETE))
      .toList();
    taskEventProcessor.processEvents(events);
    sample.stop(timer2);
  }
}
