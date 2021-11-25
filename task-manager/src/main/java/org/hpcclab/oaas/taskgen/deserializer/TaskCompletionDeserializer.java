package org.hpcclab.oaas.taskgen.deserializer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.hpcclab.oaas.model.task.TaskCompletion;

@RegisterForReflection(targets = TaskCompletion.class)
public class TaskCompletionDeserializer extends ObjectMapperDeserializer<TaskCompletion> {
  public TaskCompletionDeserializer() {
    super(TaskCompletion.class);
  }
}
