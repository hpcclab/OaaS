package org.hpcclab.oaas.orbit;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import org.hpcclab.oaas.model.task.OTask;

public class TaskDeserializer extends ObjectMapperDeserializer<OTask> {
  public TaskDeserializer() {
    super(OTask.class);
  }
}
