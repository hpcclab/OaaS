package org.hpcclab.msc.taskgen.deserializer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import org.hpcclab.msc.object.entity.task.TaskCompletion;
import org.hpcclab.msc.object.model.ObjectResourceRequest;

public class TaskCompletionDeserializer extends ObjectMapperDeserializer<TaskCompletion> {
  public TaskCompletionDeserializer() {
    super(TaskCompletion.class);
  }
}
