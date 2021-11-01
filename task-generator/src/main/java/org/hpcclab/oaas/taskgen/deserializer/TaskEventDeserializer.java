package org.hpcclab.oaas.taskgen.deserializer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import org.hpcclab.oaas.entity.task.TaskCompletion;
import org.hpcclab.oaas.model.TaskEvent;

public class TaskEventDeserializer extends ObjectMapperDeserializer<TaskEvent> {
  public TaskEventDeserializer() {
    super(TaskEvent.class);
  }
}
