package org.hpcclab.oaas.taskgen.deserializer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import org.hpcclab.oaas.model.task.TaskCompletion;

public class TaskCompletionDeserializer extends ObjectMapperDeserializer<TaskCompletion> {
  public TaskCompletionDeserializer() {
    super(TaskCompletion.class);
  }
}
