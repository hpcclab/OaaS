package org.hpcclab.oaas.invoker;


import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.mutiny.kafka.client.producer.KafkaProducer;
import io.vertx.mutiny.kafka.client.producer.KafkaProducerRecord;
import org.hpcclab.oaas.invocation.TaskFactory;
import org.hpcclab.oaas.invocation.TaskSubmitter;
import org.hpcclab.oaas.model.task.TaskContext;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Dependent
public class KafkaTaskSubmitter implements TaskSubmitter {

  @Inject
  KafkaProducer<String, Buffer> producer;
  @Inject
  TaskFactory taskFactory;
  @Inject
  InvokerConfig config;

  @Override
  public Uni<Void> submit(TaskContext context) {
    var task = taskFactory.genTask(context);
    var topic = selectTopic(context);
    var key = task.isImmutable()? null: task.getPartKey();
    var record = KafkaProducerRecord.create(
        topic,
        key,
        Json.encodeToBuffer(task)
      )
      .addHeader("ce_id", task.getId())
      .addHeader("ce_function", context.getFunction().getKey());
    return producer.send(record)
      .replaceWithVoid();
  }

  public String selectTopic(TaskContext context) {
    return config.functionTopicPrefix() + context.getFunction().getKey();
  }
}
