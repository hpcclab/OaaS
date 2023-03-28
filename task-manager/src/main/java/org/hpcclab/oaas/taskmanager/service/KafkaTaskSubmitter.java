package org.hpcclab.oaas.taskmanager.service;


import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.hpcclab.oaas.invocation.TaskFactory;
import org.hpcclab.oaas.invocation.TaskSubmitter;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskContext;
import org.hpcclab.oaas.taskmanager.TaskManagerConfig;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class KafkaTaskSubmitter implements TaskSubmitter {

  @Channel("tasks")
  MutinyEmitter<OaasTask> taskEmitter;
  @Inject
  TaskFactory taskFactory;
  @Inject
  TaskManagerConfig config;

  @Override
  public Uni<Void> submit(TaskContext context) {
    var task = taskFactory.genTask(context);
    var metaBuilder = OutgoingKafkaRecordMetadata.builder()
      .withHeaders(new RecordHeaders()
        .add("ce_id", task.getId().getBytes())
        .add("ce_function", context.getFunction().getKey().getBytes())
        .add("ce_type", OaasTask.CE_TYPE.getBytes())
      )
      .withTopic(selectTopic(context));
    if (!context.isImmutable())
      metaBuilder = metaBuilder.withKey(task.getPartKey());
    var message = Message.of(task)
      .addMetadata(metaBuilder.build());
    return taskEmitter.sendMessage(message);
  }

  public String selectTopic(TaskContext context) {
//    return config.functionTopicPrefix() + context.getFunction().getKey();
    return config.invokeTopicPrefix() + context.getMain().getCls();
  }
}
