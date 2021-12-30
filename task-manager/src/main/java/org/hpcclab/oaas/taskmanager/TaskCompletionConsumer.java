package org.hpcclab.oaas.taskmanager;

import io.micrometer.core.annotation.Timed;
import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.kafka.KafkaRecordBatch;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.hpcclab.oaas.taskmanager.service.TaskEventManager;
import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class TaskCompletionConsumer {
  private static final Logger LOGGER = LoggerFactory.getLogger( TaskCompletionConsumer.class );

  @Inject
  TaskEventManager taskEventManager;
  @Remote("TaskCompletion")
  RemoteCache<UUID, TaskCompletion> remoteCache;

  @Incoming("task-completions")
  public Uni<Void> handle(List<TaskCompletion> taskCompletions) {
    var map = taskCompletions.stream()
      .collect(Collectors.toMap(tc -> UUID.fromString(tc.getId()), Function.identity()));
    return Uni.createFrom()
      .completionStage(remoteCache.putAllAsync(map))
      .flatMap(ignore -> taskEventManager.submitCompletionEvent(taskCompletions));
  }

//  @Incoming("task-completions")
//  @Blocking
//  public void handleBlocking(List<TaskCompletion> taskCompletions) {
//    var map = taskCompletions.stream()
//      .collect(Collectors.toMap(tc -> UUID.fromString(tc.getId()), Function.identity()));
//    remoteCache.putAll(map);
//    taskEventManager.submitCompletionEventBlocking(taskCompletions);
//  }
}
