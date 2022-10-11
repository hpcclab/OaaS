package org.hpcclab.oaas.taskmanager.service;


import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import io.smallrye.reactive.messaging.kafka.Record;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hpcclab.oaas.invocation.TaskFactory;
import org.hpcclab.oaas.invocation.function.TaskSubmitter;
import org.hpcclab.oaas.model.TaskContext;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.task.OaasTask;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class KafkaTaskSubmitter implements TaskSubmitter {

  @Channel("tasks")
  MutinyEmitter<OaasTask> taskEmitter;
  @Inject
  TaskFactory taskFactory;

  @Override
  public Uni<Void> submit(TaskContext context) {
    var task = taskFactory.genTask(context);
    var message = Message.of(task)
      .addMetadata( OutgoingKafkaRecordMetadata.builder()
        .withHeaders(new RecordHeaders()
          .add("ce_id", task.getId().getBytes())
          .add("ce_function", context.getFunction().getName().getBytes())
        )
        .build());
    return taskEmitter.sendMessage(message);
  }
}
